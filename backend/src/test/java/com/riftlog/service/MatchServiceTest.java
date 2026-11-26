package com.riftlog.service;

import static com.riftlog.support.TestData.guestSeat;
import static com.riftlog.support.TestData.liveGame;
import static com.riftlog.support.TestData.match;
import static com.riftlog.support.TestData.userSeat;
import static com.riftlog.support.TestData.withDetails;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.riftlog.dto.MatchRequest;
import com.riftlog.dto.MatchRequest.GameData;
import com.riftlog.dto.MatchRequest.GamePlayerData;
import com.riftlog.dto.MatchRequest.PlayerSlot;
import com.riftlog.dto.MatchResponse;
import com.riftlog.dto.Saved;
import com.riftlog.entity.Card;
import com.riftlog.entity.GameEndReason;
import com.riftlog.entity.GameResult;
import com.riftlog.entity.MatchFormat;
import com.riftlog.entity.MatchSource;
import com.riftlog.entity.User;
import com.riftlog.exception.BadRequestException;
import com.riftlog.exception.ForbiddenException;
import com.riftlog.exception.InvalidMatchException;
import com.riftlog.repository.GuestPlayerRepository;
import com.riftlog.repository.MatchRepository;
import com.riftlog.support.TestData;

import jakarta.persistence.EntityManager;

/** Runs against the H2 schema built by the real Flyway migrations, so constraints are checked too. */
@SpringBootTest
@Transactional
class MatchServiceTest {

    @Autowired
    private MatchService matchService;
    @Autowired
    private MatchRepository matchRepository;
    @Autowired
    private GuestPlayerRepository guestPlayerRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private TestData data;

    private User paul;
    private User alex;
    private Card vi;
    private Card leblanc;
    private Card starSpring;

    @BeforeEach
    void setUp() {
        paul = data.user("Paul");
        alex = data.user("Alex");
        vi = data.card("test-001", "Vi - Piltover Enforcer", Card.TYPE_LEGEND);
        leblanc = data.card("test-002", "LeBlanc - Deceiver", Card.TYPE_LEGEND);
        starSpring = data.card("test-003", "Star Spring", Card.TYPE_BATTLEFIELD);
    }

    private List<PlayerSlot> paulVsThomas() {
        return List.of(userSeat(1, paul, vi), guestSeat(2, "Thomas", leblanc));
    }

    /** Writes to the database and reads back, so mappings and SQL constraints are really exercised. */
    private MatchResponse reload(UUID id) {
        entityManager.flush();
        entityManager.clear();
        return matchService.get(id);
    }

    @Test
    void liveBo3_scoresAndWinnerAreDerivedFromThePoints() {
        UUID id = UUID.randomUUID();
        MatchRequest request = match(MatchFormat.BO3, paulVsThomas(),
                liveGame(1, 1, GameResult.SEAT1, 8, 5),
                liveGame(2, 2, GameResult.SEAT2, 6, 8),
                liveGame(3, 1, GameResult.SEAT1, 8, 7));

        Saved<MatchResponse> saved = matchService.upsert(id, request, paul.getId());
        MatchResponse match = reload(id);

        assertTrue(saved.created());
        assertEquals(1, match.winnerSeat());
        assertEquals(List.of(2, 1), match.gameWins());
        assertEquals(3, match.games().size());
        assertEquals("Thomas", match.players().get(1).name());
        assertEquals("Vi - Piltover Enforcer", match.players().get(0).legend().name());

        MatchResponse.GameView game1 = match.games().get(0);
        assertEquals(8, game1.players().get(0).finalScore());
        assertEquals(5, game1.players().get(1).finalScore());
        assertEquals(13, game1.events().size());
        assertEquals(1, game1.events().get(game1.events().size() - 1).seat());
        assertEquals(GameEndReason.POINTS, game1.endReason());
        assertEquals(14 * 30L, game1.durationSeconds());
    }

    @Test
    void sendingTheSameMatchAgainReplacesIt() {
        UUID id = UUID.randomUUID();
        GameData game = liveGame(1, 1, GameResult.SEAT2, 4, 8);
        matchService.upsert(id, match(MatchFormat.BO1, paulVsThomas(), game), paul.getId());
        entityManager.flush();

        GameData completed = withDetails(game, new GamePlayerData(1, null, starSpring.getId(), 1, "Flooded"));
        Saved<MatchResponse> second = matchService.upsert(id, match(MatchFormat.BO1, paulVsThomas(), completed),
                paul.getId());
        MatchResponse match = reload(id);

        assertFalse(second.created());
        assertEquals(1, matchRepository.count());
        assertEquals(1, guestPlayerRepository.count());
        assertEquals("Star Spring", match.games().get(0).players().get(0).battlefield().name());
        assertEquals(1, match.games().get(0).players().get(0).mulliganCount());
        assertEquals(2, match.winnerSeat());
    }

    @Test
    void manualMatch_usesTypedScores() {
        UUID id = UUID.randomUUID();
        GameData typed = new GameData(UUID.randomUUID(), 1, 2, null, null, GameResult.SEAT1, null,
                List.of(new GamePlayerData(1, 8, null, null, null), new GamePlayerData(2, 3, null, null, null)),
                List.of(), List.of());
        MatchRequest request = new MatchRequest(MatchFormat.BO1, MatchSource.MANUAL, null, TestData.T0,
                paulVsThomas(), List.of(typed));

        matchService.upsert(id, request, paul.getId());
        MatchResponse match = reload(id);

        assertEquals(8, match.games().get(0).players().get(0).finalScore());
        assertEquals(3, match.games().get(0).players().get(1).finalScore());
        assertNull(match.games().get(0).durationSeconds());
    }

    @Test
    void drawnGame_leavesTheMatchWithoutWinner() {
        UUID id = UUID.randomUUID();
        matchService.upsert(id, match(MatchFormat.BO1, paulVsThomas(), liveGame(1, 1, GameResult.DRAW, 5, 5)),
                paul.getId());

        assertNull(reload(id).winnerSeat());
    }

    @Test
    void rejectsAGamePlayedAfterTheMatchWasWon() {
        MatchRequest request = match(MatchFormat.BO3, paulVsThomas(),
                liveGame(1, 1, GameResult.SEAT1, 8, 2),
                liveGame(2, 2, GameResult.SEAT1, 8, 3),
                liveGame(3, 1, GameResult.SEAT2, 1, 8));

        assertThrows(InvalidMatchException.class, () -> matchService.upsert(UUID.randomUUID(), request, paul.getId()));
    }

    @Test
    void rejectsASecondGameInABo1() {
        MatchRequest request = match(MatchFormat.BO1, paulVsThomas(),
                liveGame(1, 1, GameResult.SEAT1, 8, 2),
                liveGame(2, 2, GameResult.SEAT2, 1, 8));

        assertThrows(InvalidMatchException.class, () -> matchService.upsert(UUID.randomUUID(), request, paul.getId()));
    }

    @Test
    void rejectsAWinOnPointsWithoutEightPoints() {
        MatchRequest request = match(MatchFormat.BO1, paulVsThomas(), liveGame(1, 1, GameResult.SEAT1, 7, 5));

        assertThrows(InvalidMatchException.class, () -> matchService.upsert(UUID.randomUUID(), request, paul.getId()));
    }

    @Test
    void rejectsABattlefieldUsedAsLegend() {
        List<PlayerSlot> players = List.of(userSeat(1, paul, starSpring), guestSeat(2, "Thomas", leblanc));
        MatchRequest request = match(MatchFormat.BO1, players, liveGame(1, 1, GameResult.SEAT1, 8, 0));

        assertThrows(BadRequestException.class, () -> matchService.upsert(UUID.randomUUID(), request, paul.getId()));
    }

    @Test
    void rejectsAPlayerAgainstThemselves() {
        List<PlayerSlot> players = List.of(userSeat(1, paul, vi), userSeat(2, paul, leblanc));
        MatchRequest request = match(MatchFormat.BO1, players, liveGame(1, 1, GameResult.SEAT1, 8, 0));

        assertThrows(InvalidMatchException.class, () -> matchService.upsert(UUID.randomUUID(), request, paul.getId()));
    }

    @Test
    void someoneWhoDidNotPlayCannotChangeTheMatch() {
        UUID id = UUID.randomUUID();
        MatchRequest request = match(MatchFormat.BO1, paulVsThomas(), liveGame(1, 1, GameResult.SEAT1, 8, 0));
        matchService.upsert(id, request, paul.getId());
        entityManager.flush();

        assertThrows(ForbiddenException.class, () -> matchService.upsert(id, request, alex.getId()));
        assertThrows(ForbiddenException.class, () -> matchService.delete(id, alex.getId()));
    }

    @Test
    void list_filtersByParticipant() {
        matchService.upsert(UUID.randomUUID(), match(MatchFormat.BO1, paulVsThomas(),
                liveGame(1, 1, GameResult.SEAT1, 8, 0)), paul.getId());
        matchService.upsert(UUID.randomUUID(), match(MatchFormat.BO1,
                List.of(userSeat(1, alex, leblanc), guestSeat(2, "Thomas", vi)),
                liveGame(1, 1, GameResult.SEAT2, 0, 8)), alex.getId());
        entityManager.flush();
        entityManager.clear();

        assertEquals(2, matchService.list(null).size());
        assertEquals(1, matchService.list(paul.getId()).size());
        assertTrue(matchService.list(paul.getId()).get(0).games().isEmpty());
    }
}
