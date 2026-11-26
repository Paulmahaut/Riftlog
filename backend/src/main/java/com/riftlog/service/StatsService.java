package com.riftlog.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.riftlog.dto.CardRef;
import com.riftlog.dto.PlayerSummary;
import com.riftlog.dto.StatsResponse;
import com.riftlog.dto.StatsResponse.BattlefieldLine;
import com.riftlog.dto.StatsResponse.Comeback;
import com.riftlog.dto.StatsResponse.LegendLine;
import com.riftlog.dto.StatsResponse.PointSplit;
import com.riftlog.dto.StatsResponse.Tally;
import com.riftlog.entity.Card;
import com.riftlog.entity.Game;
import com.riftlog.entity.GamePlayer;
import com.riftlog.entity.GameResult;
import com.riftlog.entity.Match;
import com.riftlog.entity.MatchPlayer;
import com.riftlog.entity.ScoreEvent;
import com.riftlog.entity.ScoreType;
import com.riftlog.entity.User;
import com.riftlog.repository.MatchRepository;

/**
 * Statistics of one registered player (SPEC §3), derived from their matches on every call.
 * The group is small, so computing in memory is simpler than SQL aggregates and fast enough.
 */
@Service
public class StatsService {

    /** Being behind by this many points (e.g. 2-5) and still winning counts as a comeback. */
    public static final int COMEBACK_DEFICIT = 3;

    private final MatchRepository matchRepository;
    private final UserService userService;

    public StatsService(MatchRepository matchRepository, UserService userService) {
        this.matchRepository = matchRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public StatsResponse forUser(Long userId) {
        User user = userService.require(userId);
        Counter matches = new Counter();
        Counter games = new Counter();
        Counter first = new Counter();
        Counter second = new Counter();
        Map<Long, LegendCounter> myLegends = new LinkedHashMap<>();
        Map<Long, LegendCounter> opponentLegends = new LinkedHashMap<>();
        Map<Long, CardCounter> myBattlefields = new LinkedHashMap<>();
        Map<Long, CardCounter> opponentBattlefields = new LinkedHashMap<>();
        int[] scored = new int[3];
        int[] conceded = new int[3];
        int countedGames = 0;
        long totalSeconds = 0;
        int timedGames = 0;
        List<Comeback> comebacks = new ArrayList<>();

        for (Match match : matchRepository.findByParticipant(userId)) {
            MatchPlayer me = match.getPlayers().stream()
                    .filter(p -> p.getUser() != null && p.getUser().getId().equals(userId))
                    .findFirst().orElseThrow();
            MatchPlayer opponent = match.getPlayers().stream().filter(p -> p != me).findFirst().orElseThrow();
            int mySeat = me.getSeat();

            matches.add(match.getWinnerSeat() == null ? 0 : match.getWinnerSeat() == mySeat ? 1 : -1);

            for (Game game : match.getGames()) {
                int outcome = outcome(game.getResult(), mySeat);
                boolean wentFirst = game.getFirstSeat() == mySeat;
                games.add(outcome);
                (wentFirst ? first : second).add(outcome);
                legend(myLegends, me.getLegend()).add(outcome, wentFirst);
                legend(opponentLegends, opponent.getLegend()).add(outcome, wentFirst);

                GamePlayer myDetails = game.player(mySeat);
                GamePlayer opponentDetails = game.player(opponent.getSeat());
                if (myDetails != null && myDetails.getBattlefield() != null) {
                    card(myBattlefields, myDetails.getBattlefield()).counter.add(outcome);
                }
                if (opponentDetails != null && opponentDetails.getBattlefield() != null) {
                    card(opponentBattlefields, opponentDetails.getBattlefield()).counter.add(outcome);
                }

                if (game.getStartedAt() != null && game.getEndedAt() != null) {
                    totalSeconds += Duration.between(game.getStartedAt(), game.getEndedAt()).getSeconds();
                    timedGames++;
                }

                if (!game.getEvents().isEmpty()) {
                    countedGames++;
                    for (ScoreEvent event : game.getEvents()) {
                        (event.getSeat() == mySeat ? scored : conceded)[event.getType().ordinal()]++;
                    }
                    if (outcome > 0) {
                        Comeback comeback = comeback(match, game, mySeat, opponent);
                        if (comeback != null) {
                            comebacks.add(comeback);
                        }
                    }
                }
            }
        }

        comebacks.sort(Comparator.comparingInt((Comeback c) -> c.scoreWhenBehind() - c.opponentScoreWhenBehind())
                .thenComparing(Comeback::playedAt, Comparator.reverseOrder()));

        return new StatsResponse(
                new PlayerSummary(user.getId(), user.getDisplayName()),
                matches.tally(), games.tally(), first.tally(), second.tally(),
                timedGames == 0 ? null : totalSeconds / timedGames,
                legendLines(myLegends), legendLines(opponentLegends),
                split(scored, countedGames), split(conceded, countedGames),
                battlefieldLines(myBattlefields), battlefieldLines(opponentBattlefields),
                comebacks);
    }

    /** 1 win, -1 loss, 0 draw, from the point of view of {@code seat}. */
    private static int outcome(GameResult result, int seat) {
        Integer winner = result.winnerSeat();
        return winner == null ? 0 : winner == seat ? 1 : -1;
    }

    /** The worst moment of a won game, if the player was ever {@link #COMEBACK_DEFICIT} points behind. */
    private static Comeback comeback(Match match, Game game, int mySeat, MatchPlayer opponent) {
        int mine = 0;
        int theirs = 0;
        int worstGap = 0;
        int[] worst = null;
        for (ScoreEvent event : game.getEvents()) {
            if (event.getSeat() == mySeat) {
                mine++;
            } else {
                theirs++;
            }
            if (theirs - mine > worstGap) {
                worstGap = theirs - mine;
                worst = new int[] {mine, theirs};
            }
        }
        if (worstGap < COMEBACK_DEFICIT) {
            return null;
        }
        return new Comeback(match.getId(), game.getNumber(), match.getPlayedAt(), opponent.displayName(),
                CardService.toRef(opponent.getLegend()), worst[0], worst[1], mine, theirs);
    }

    private static PointSplit split(int[] points, int games) {
        return new PointSplit(points[ScoreType.CONQUER.ordinal()], points[ScoreType.HOLD.ordinal()],
                points[ScoreType.ABILITY.ordinal()], games);
    }

    private static LegendCounter legend(Map<Long, LegendCounter> map, Card card) {
        return map.computeIfAbsent(card.getId(), id -> new LegendCounter(CardService.toRef(card)));
    }

    private static CardCounter card(Map<Long, CardCounter> map, Card card) {
        return map.computeIfAbsent(card.getId(), id -> new CardCounter(CardService.toRef(card)));
    }

    private static List<LegendLine> legendLines(Map<Long, LegendCounter> map) {
        return sorted(map.values(), c -> c.all, c -> new LegendLine(c.card, c.all.tally(), c.first.tally(),
                c.second.tally()), false);
    }

    /** Battlefields: the ones lost on most come first. */
    private static List<BattlefieldLine> battlefieldLines(Map<Long, CardCounter> map) {
        return sorted(map.values(), c -> c.counter, c -> new BattlefieldLine(c.card, c.counter.tally()), true);
    }

    private static <C, R> List<R> sorted(Iterable<C> counters, Function<C, Counter> key, Function<C, R> toLine,
            boolean lossesFirst) {
        List<C> list = new ArrayList<>();
        counters.forEach(list::add);
        Comparator<C> order = lossesFirst
                ? Comparator.comparingInt((C c) -> key.apply(c).losses).reversed()
                        .thenComparing(Comparator.comparingInt((C c) -> key.apply(c).played).reversed())
                : Comparator.comparingInt((C c) -> key.apply(c).played).reversed()
                        .thenComparing(Comparator.comparingInt((C c) -> key.apply(c).wins).reversed());
        list.sort(order);
        return list.stream().map(toLine).toList();
    }

    // ------------------------------------------------------------------

    private static final class Counter {
        int played;
        int wins;
        int losses;
        int draws;

        void add(int outcome) {
            played++;
            if (outcome > 0) {
                wins++;
            } else if (outcome < 0) {
                losses++;
            } else {
                draws++;
            }
        }

        Tally tally() {
            return new Tally(played, wins, losses, draws);
        }
    }

    private static final class LegendCounter {
        final CardRef card;
        final Counter all = new Counter();
        final Counter first = new Counter();
        final Counter second = new Counter();

        LegendCounter(CardRef card) {
            this.card = card;
        }

        void add(int outcome, boolean wentFirst) {
            all.add(outcome);
            (wentFirst ? first : second).add(outcome);
        }
    }

    private static final class CardCounter {
        final CardRef card;
        final Counter counter = new Counter();

        CardCounter(CardRef card) {
            this.card = card;
        }
    }
}
