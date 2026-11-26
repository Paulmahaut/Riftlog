package com.riftlog.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.riftlog.dto.MatchRequest;
import com.riftlog.dto.MatchRequest.EventData;
import com.riftlog.dto.MatchRequest.GameData;
import com.riftlog.dto.MatchRequest.GamePlayerData;
import com.riftlog.dto.MatchRequest.PlayerSlot;
import com.riftlog.dto.MatchRequest.SideChangeData;
import com.riftlog.dto.MatchResponse;
import com.riftlog.dto.Saved;
import com.riftlog.entity.Card;
import com.riftlog.entity.Deck;
import com.riftlog.entity.Game;
import com.riftlog.entity.GameEndReason;
import com.riftlog.entity.GamePlayer;
import com.riftlog.entity.GameResult;
import com.riftlog.entity.GameSideChange;
import com.riftlog.entity.Match;
import com.riftlog.entity.MatchPlayer;
import com.riftlog.entity.MatchSource;
import com.riftlog.entity.ScoreEvent;
import com.riftlog.entity.User;
import com.riftlog.exception.BadRequestException;
import com.riftlog.exception.ForbiddenException;
import com.riftlog.exception.InvalidMatchException;
import com.riftlog.exception.NotFoundException;
import com.riftlog.repository.DeckRepository;
import com.riftlog.repository.MatchRepository;
import com.riftlog.repository.UserRepository;

@Service
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);
    static final int WINNING_SCORE = 8;
    private static final int[] SEATS = {1, 2};

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final UserService userService;
    private final GuestService guestService;
    private final CardService cardService;

    public MatchService(MatchRepository matchRepository, UserRepository userRepository, DeckRepository deckRepository,
            UserService userService, GuestService guestService, CardService cardService) {
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.deckRepository = deckRepository;
        this.userService = userService;
        this.guestService = guestService;
        this.cardService = cardService;
    }

    // ------------------------------------------------------------------
    // Write
    // ------------------------------------------------------------------

    /**
     * Stores a whole match under the id chosen by the client. Sending it again replaces it:
     * a retried upload never duplicates a match, and post-game details can be added later.
     */
    @Transactional
    public Saved<MatchResponse> upsert(UUID id, MatchRequest request, Long currentUserId) {
        User currentUser = userService.require(currentUserId);
        Match existing = matchRepository.findById(id).orElse(null);
        if (existing != null) {
            ensureCanEdit(existing, currentUserId);
        }

        List<GameData> games = validate(request);
        Match match = build(id, request, games, currentUser);

        if (existing != null) {
            match.setRecordedBy(existing.getRecordedBy());
            match.setCreatedAt(existing.getCreatedAt());
            // Rows keep their client ids, so the old version must be gone before the new one is inserted.
            matchRepository.delete(existing);
            matchRepository.flush();
        }

        Match saved = matchRepository.save(match);
        log.info("User {} {} match {} ({} {}, {} games, winner seat {})", currentUserId,
                existing == null ? "recorded" : "updated", saved.getId(), saved.getSource(), saved.getFormat(),
                saved.getGames().size(), saved.getWinnerSeat());
        return new Saved<>(toResponse(saved, true), existing == null);
    }

    @Transactional
    public void delete(UUID id, Long currentUserId) {
        Match match = require(id);
        ensureCanEdit(match, currentUserId);
        matchRepository.delete(match);
        log.info("User {} deleted match {}", currentUserId, id);
    }

    /** Whoever recorded the match, or either registered player, may change it. */
    private static void ensureCanEdit(Match match, Long userId) {
        boolean recorder = match.getRecordedBy().getId().equals(userId);
        boolean participant = match.getPlayers().stream()
                .anyMatch(player -> player.getUser() != null && player.getUser().getId().equals(userId));
        if (!recorder && !participant) {
            throw new ForbiddenException("Only the players of match " + match.getId() + " can change it");
        }
    }

    // ------------------------------------------------------------------
    // Read
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public MatchResponse get(UUID id) {
        return toResponse(require(id), true);
    }

    /** All matches of the group (everyone can see everyone), or those one player took part in. */
    @Transactional(readOnly = true)
    public List<MatchResponse> list(Long userId) {
        List<Match> matches = userId == null
                ? matchRepository.findAllByOrderByPlayedAtDesc()
                : matchRepository.findByParticipant(userId);
        return matches.stream().map(match -> toResponse(match, false)).toList();
    }

    private Match require(UUID id) {
        return matchRepository.findById(id).orElseThrow(() -> new NotFoundException("Match " + id + " not found"));
    }

    // ------------------------------------------------------------------
    // Validation (structure only: no database access, nothing is created)
    // ------------------------------------------------------------------

    /** Returns the games sorted by number. */
    private static List<GameData> validate(MatchRequest request) {
        validateSeats(request.players());

        List<GameData> games = request.games().stream().sorted(Comparator.comparingInt(GameData::number)).toList();
        if (games.size() > request.format().maxGames()) {
            throw new InvalidMatchException("A " + request.format() + " has at most " + request.format().maxGames()
                    + " game(s), got " + games.size());
        }

        Set<UUID> ids = new HashSet<>();
        int[] wins = new int[3];
        for (int i = 0; i < games.size(); i++) {
            GameData game = games.get(i);
            if (game.number() != i + 1) {
                throw new InvalidMatchException("Games must be numbered 1 to " + games.size());
            }
            if (wins[1] >= request.format().gamesToWin() || wins[2] >= request.format().gamesToWin()) {
                throw new InvalidMatchException("Game " + game.number() + " was played after the match was already won");
            }
            requireUniqueId(ids, game.id(), "game");
            validateGame(game, request.source(), ids);

            Integer winner = game.result().winnerSeat();
            if (winner != null) {
                wins[winner]++;
            }
        }
        return games;
    }

    private static void validateSeats(List<PlayerSlot> players) {
        Set<Integer> seats = new HashSet<>();
        for (PlayerSlot slot : players) {
            if (!seats.add(slot.seat())) {
                throw new InvalidMatchException("Seat " + slot.seat() + " is listed twice");
            }
            int identities = (slot.userId() != null ? 1 : 0) + (slot.guestId() != null ? 1 : 0)
                    + (slot.guestName() != null && !slot.guestName().isBlank() ? 1 : 0);
            if (identities != 1) {
                throw new InvalidMatchException("Seat " + slot.seat() + " needs exactly one of userId, guestId or guestName");
            }
            if (slot.deckId() == null && slot.legendCardId() == null) {
                throw new InvalidMatchException("Seat " + slot.seat() + " needs a legend (legendCardId or deckId)");
            }
        }
        Long user1 = players.get(0).userId();
        Long user2 = players.get(1).userId();
        if (user1 != null && user1.equals(user2)) {
            throw new InvalidMatchException("A player cannot play against themselves");
        }
    }

    private static void validateGame(GameData game, MatchSource source, Set<UUID> ids) {
        String label = "Game " + game.number();
        if (game.startedAt() != null && game.endedAt() != null && game.endedAt().isBefore(game.startedAt())) {
            throw new InvalidMatchException(label + " ends before it starts");
        }

        List<EventData> events = sortedEvents(game);
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).seq() != i + 1) {
                throw new InvalidMatchException(label + ": points must be numbered 1 to " + events.size());
            }
            requireUniqueId(ids, events.get(i).id(), "point");
        }

        Set<Integer> detailSeats = new HashSet<>();
        for (GamePlayerData data : nullSafe(game.players())) {
            if (!detailSeats.add(data.seat())) {
                throw new InvalidMatchException(label + ": seat " + data.seat() + " details are listed twice");
            }
        }
        for (SideChangeData change : nullSafe(game.sideChanges())) {
            if (change.delta() == 0) {
                throw new InvalidMatchException(label + ": a side change cannot be 0");
            }
        }

        GameEndReason reason = endReason(game);
        boolean draw = game.result() == GameResult.DRAW;
        if (draw != (reason == GameEndReason.DRAW)) {
            throw new InvalidMatchException(label + ": result " + game.result() + " does not match end reason " + reason);
        }

        if (reason == GameEndReason.POINTS) {
            boolean usesEvents = usesEvents(game, source);
            if (!usesEvents && (finalScore(game, 1) == null || finalScore(game, 2) == null)) {
                throw new InvalidMatchException(label + ": both final scores are required when there are no points");
            }
            int winner = game.result().winnerSeat();
            int winnerScore = score(game, source, winner);
            int loserScore = score(game, source, 3 - winner);
            if (winnerScore < WINNING_SCORE || loserScore >= WINNING_SCORE) {
                throw new InvalidMatchException(label + " was won on points but the score is " + score(game, source, 1)
                        + "-" + score(game, source, 2));
            }
        }
    }

    private static void requireUniqueId(Set<UUID> ids, UUID id, String what) {
        if (!ids.add(id)) {
            throw new InvalidMatchException("The same id is used twice (" + what + " " + id + ")");
        }
    }

    // ------------------------------------------------------------------
    // Scores: always derived from the points when the match was counted live
    // ------------------------------------------------------------------

    private static boolean usesEvents(GameData game, MatchSource source) {
        return source == MatchSource.LIVE || !nullSafe(game.events()).isEmpty();
    }

    private static int score(GameData game, MatchSource source, int seat) {
        if (usesEvents(game, source)) {
            return (int) nullSafe(game.events()).stream().filter(event -> event.seat() == seat).count();
        }
        Integer typed = finalScore(game, seat);
        return typed == null ? 0 : typed;
    }

    private static Integer finalScore(GameData game, int seat) {
        return nullSafe(game.players()).stream().filter(data -> data.seat() == seat).findFirst()
                .map(GamePlayerData::finalScore).orElse(null);
    }

    private static GameEndReason endReason(GameData game) {
        if (game.endReason() != null) {
            return game.endReason();
        }
        return game.result() == GameResult.DRAW ? GameEndReason.DRAW : GameEndReason.POINTS;
    }

    private static List<EventData> sortedEvents(GameData game) {
        return nullSafe(game.events()).stream().sorted(Comparator.comparingInt(EventData::seq)).toList();
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }

    // ------------------------------------------------------------------
    // Building entities (resolves users, guests, decks and cards)
    // ------------------------------------------------------------------

    private Match build(UUID id, MatchRequest request, List<GameData> games, User currentUser) {
        Instant now = Instant.now();
        Match match = new Match();
        match.setId(id);
        match.setFormat(request.format());
        match.setSource(request.source());
        match.setExternalId(request.externalId());
        match.setRecordedBy(currentUser);
        match.setPlayedAt(request.playedAt());
        match.setCreatedAt(now);
        match.setUpdatedAt(now);

        request.players().stream()
                .sorted(Comparator.comparingInt(PlayerSlot::seat))
                .forEach(slot -> match.getPlayers().add(buildPlayer(match, slot, currentUser)));

        int[] wins = new int[3];
        for (GameData data : games) {
            Game game = buildGame(match, data, request.source());
            match.getGames().add(game);
            Integer winner = data.result().winnerSeat();
            if (winner != null) {
                wins[winner]++;
            }
        }
        match.setWinnerSeat(wins[1] > wins[2] ? Integer.valueOf(1) : wins[2] > wins[1] ? Integer.valueOf(2) : null);
        return match;
    }

    private MatchPlayer buildPlayer(Match match, PlayerSlot slot, User currentUser) {
        String label = "Seat " + slot.seat();
        MatchPlayer player = new MatchPlayer();
        player.setMatch(match);
        player.setSeat(slot.seat());

        if (slot.userId() != null) {
            player.setUser(userRepository.findById(slot.userId())
                    .orElseThrow(() -> new BadRequestException(label + ": unknown user " + slot.userId())));
        } else if (slot.guestId() != null) {
            player.setGuest(guestService.require(slot.guestId()));
        } else {
            player.setGuest(guestService.findOrCreate(slot.guestName(), currentUser));
        }

        Deck deck = null;
        if (slot.deckId() != null) {
            deck = deckRepository.findById(slot.deckId())
                    .orElseThrow(() -> new BadRequestException(label + ": unknown deck " + slot.deckId()));
            if (player.getUser() == null || !deck.getOwner().getId().equals(player.getUser().getId())) {
                throw new InvalidMatchException(label + ": deck " + deck.getName() + " belongs to another player");
            }
            player.setDeck(deck);
        }

        if (slot.legendCardId() != null) {
            Card legend = cardService.require(slot.legendCardId(), Card.TYPE_LEGEND, label + " legend");
            if (deck != null && !deck.getLegend().getBaseName().equalsIgnoreCase(legend.getBaseName())) {
                throw new InvalidMatchException(label + ": legend " + legend.getBaseName() + " does not match deck "
                        + deck.getName() + " (" + deck.getLegend().getBaseName() + ")");
            }
            player.setLegend(legend);
        } else {
            player.setLegend(deck.getLegend());
        }
        return player;
    }

    private Game buildGame(Match match, GameData data, MatchSource source) {
        String label = "Game " + data.number();
        Game game = new Game();
        game.setId(data.id());
        game.setMatch(match);
        game.setNumber(data.number());
        game.setFirstSeat(data.firstSeat());
        game.setStartedAt(data.startedAt());
        game.setEndedAt(data.endedAt());
        game.setResult(data.result());
        game.setEndReason(endReason(data));

        Map<Integer, GamePlayerData> details = new HashMap<>();
        nullSafe(data.players()).forEach(detail -> details.put(detail.seat(), detail));
        for (int seat : SEATS) {
            GamePlayerData detail = details.get(seat);
            GamePlayer player = new GamePlayer();
            player.setGame(game);
            player.setSeat(seat);
            player.setFinalScore(score(data, source, seat));
            if (detail != null) {
                if (detail.battlefieldCardId() != null) {
                    player.setBattlefield(cardService.require(detail.battlefieldCardId(), Card.TYPE_BATTLEFIELD,
                            label + " seat " + seat + " battlefield"));
                }
                player.setMulliganCount(detail.mulliganCount());
                player.setNotes(detail.notes());
            }
            game.getPlayers().add(player);
        }

        for (SideChangeData changeData : nullSafe(data.sideChanges())) {
            GameSideChange change = new GameSideChange();
            change.setGame(game);
            change.setSeat(changeData.seat());
            change.setCard(cardService.require(changeData.cardId(), null, label + " side change"));
            change.setDelta(changeData.delta());
            game.getSideChanges().add(change);
        }

        for (EventData eventData : sortedEvents(data)) {
            ScoreEvent event = new ScoreEvent();
            event.setId(eventData.id());
            event.setGame(game);
            event.setSeq(eventData.seq());
            event.setSeat(eventData.seat());
            event.setType(eventData.type());
            event.setOccurredAt(eventData.occurredAt());
            game.getEvents().add(event);
        }
        return game;
    }

    // ------------------------------------------------------------------
    // Mapping
    // ------------------------------------------------------------------

    private static MatchResponse toResponse(Match match, boolean withGames) {
        List<MatchResponse.PlayerView> players = match.getPlayers().stream()
                .map(player -> new MatchResponse.PlayerView(
                        player.getSeat(),
                        player.getUser() == null ? null : player.getUser().getId(),
                        player.getGuest() == null ? null : player.getGuest().getId(),
                        player.displayName(),
                        CardService.toRef(player.getLegend()),
                        player.getDeck() == null ? null : player.getDeck().getId(),
                        player.getDeck() == null ? null : player.getDeck().getName()))
                .toList();

        long seat1Wins = match.getGames().stream().filter(game -> game.getResult() == GameResult.SEAT1).count();
        long seat2Wins = match.getGames().stream().filter(game -> game.getResult() == GameResult.SEAT2).count();

        List<MatchResponse.GameView> games = withGames
                ? match.getGames().stream().map(MatchService::toGameView).toList()
                : new ArrayList<>();

        return new MatchResponse(match.getId(), match.getFormat(), match.getSource(), match.getExternalId(),
                match.getPlayedAt(), match.getRecordedBy().getId(), match.getWinnerSeat(),
                List.of((int) seat1Wins, (int) seat2Wins), match.getGames().size(), players, games);
    }

    private static MatchResponse.GameView toGameView(Game game) {
        Long duration = game.getStartedAt() != null && game.getEndedAt() != null
                ? Duration.between(game.getStartedAt(), game.getEndedAt()).getSeconds()
                : null;
        List<MatchResponse.GamePlayerView> players = game.getPlayers().stream()
                .map(player -> new MatchResponse.GamePlayerView(player.getSeat(), player.getFinalScore(),
                        CardService.toRef(player.getBattlefield()), player.getMulliganCount(), player.getNotes()))
                .toList();
        List<MatchResponse.SideChangeView> sideChanges = game.getSideChanges().stream()
                .map(change -> new MatchResponse.SideChangeView(change.getSeat(), CardService.toRef(change.getCard()),
                        change.getDelta()))
                .toList();
        List<MatchResponse.EventView> events = game.getEvents().stream()
                .map(event -> new MatchResponse.EventView(event.getId(), event.getSeq(), event.getSeat(),
                        event.getType(), event.getOccurredAt()))
                .toList();
        return new MatchResponse.GameView(game.getId(), game.getNumber(), game.getFirstSeat(), game.getStartedAt(),
                game.getEndedAt(), duration, game.getResult(), game.getEndReason(), players, sideChanges, events);
    }
}
