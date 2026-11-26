package com.riftlog.support;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.riftlog.dto.MatchRequest;
import com.riftlog.dto.MatchRequest.EventData;
import com.riftlog.dto.MatchRequest.GameData;
import com.riftlog.dto.MatchRequest.GamePlayerData;
import com.riftlog.dto.MatchRequest.PlayerSlot;
import com.riftlog.entity.Card;
import com.riftlog.entity.GameEndReason;
import com.riftlog.entity.GameResult;
import com.riftlog.entity.MatchFormat;
import com.riftlog.entity.MatchSource;
import com.riftlog.entity.ScoreType;
import com.riftlog.entity.User;
import com.riftlog.repository.CardRepository;
import com.riftlog.repository.UserRepository;

/** Builders for integration tests. Everything is rolled back by @Transactional tests. */
@Component
public class TestData {

    public static final Instant T0 = Instant.parse("2026-11-26T18:00:00Z");

    private final UserRepository userRepository;
    private final CardRepository cardRepository;

    public TestData(UserRepository userRepository, CardRepository cardRepository) {
        this.userRepository = userRepository;
        this.cardRepository = cardRepository;
    }

    public User user(String displayName) {
        User user = new User();
        user.setEmail(displayName.toLowerCase() + "@example.com");
        user.setPasswordHash("not-used");
        user.setDisplayName(displayName);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public Card card(String riftboundId, String name, String type) {
        Card card = new Card();
        card.setRiftcodexId("rc-" + riftboundId);
        card.setRiftboundId(riftboundId);
        card.setName(name);
        card.setBaseName(name);
        card.setCardType(type);
        card.setSetId("OGN");
        card.setVariant(false);
        card.setCanonical(true);
        card.setUpdatedAt(Instant.now());
        return cardRepository.save(card);
    }

    public static PlayerSlot userSeat(int seat, User user, Card legend) {
        return new PlayerSlot(seat, user.getId(), null, null, null, legend.getId());
    }

    public static PlayerSlot guestSeat(int seat, String guestName, Card legend) {
        return new PlayerSlot(seat, null, null, guestName, null, legend.getId());
    }

    /**
     * A game counted live: points alternate between seats, and the winner's last point comes last.
     * Duration is 30 seconds per point.
     */
    public static GameData liveGame(int number, int firstSeat, GameResult result, int seat1Points, int seat2Points) {
        int winner = result.winnerSeat() == null ? 0 : result.winnerSeat();
        boolean holdBackLastPoint = winner == 1 ? seat1Points > 0 : winner == 2 && seat2Points > 0;
        int remaining1 = seat1Points - (holdBackLastPoint && winner == 1 ? 1 : 0);
        int remaining2 = seat2Points - (holdBackLastPoint && winner == 2 ? 1 : 0);
        List<Integer> seats = new ArrayList<>();
        while (remaining1 > 0 || remaining2 > 0) {
            if (remaining1 > 0) {
                seats.add(1);
                remaining1--;
            }
            if (remaining2 > 0) {
                seats.add(2);
                remaining2--;
            }
        }
        if (holdBackLastPoint) {
            seats.add(winner);
        }

        Instant start = T0.plusSeconds(3600L * number);
        List<EventData> events = new ArrayList<>();
        ScoreType[] types = ScoreType.values();
        for (int i = 0; i < seats.size(); i++) {
            events.add(new EventData(UUID.randomUUID(), i + 1, seats.get(i), types[i % types.length],
                    start.plusSeconds(30L * (i + 1))));
        }
        GameEndReason reason = result == GameResult.DRAW ? GameEndReason.DRAW : GameEndReason.POINTS;
        return new GameData(UUID.randomUUID(), number, firstSeat, start, start.plusSeconds(30L * (seats.size() + 1)),
                result, reason, List.of(), List.of(), events);
    }

    /** Same game with post-game details added for one seat. */
    public static GameData withDetails(GameData game, GamePlayerData... details) {
        return new GameData(game.id(), game.number(), game.firstSeat(), game.startedAt(), game.endedAt(),
                game.result(), game.endReason(), List.of(details), game.sideChanges(), game.events());
    }

    public static MatchRequest match(MatchFormat format, List<PlayerSlot> players, GameData... games) {
        return new MatchRequest(format, MatchSource.LIVE, null, T0, players, List.of(games));
    }
}
