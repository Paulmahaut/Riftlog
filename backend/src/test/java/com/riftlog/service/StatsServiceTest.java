package com.riftlog.service;

import static com.riftlog.support.TestData.guestSeat;
import static com.riftlog.support.TestData.liveGame;
import static com.riftlog.support.TestData.match;
import static com.riftlog.support.TestData.scriptedGame;
import static com.riftlog.support.TestData.userSeat;
import static com.riftlog.support.TestData.withDetails;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.riftlog.dto.MatchRequest.GamePlayerData;
import com.riftlog.dto.StatsResponse;
import com.riftlog.dto.StatsResponse.Comeback;
import com.riftlog.dto.StatsResponse.Tally;
import com.riftlog.entity.Card;
import com.riftlog.entity.GameResult;
import com.riftlog.entity.MatchFormat;
import com.riftlog.entity.User;
import com.riftlog.support.TestData;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class StatsServiceTest {

    @Autowired
    private StatsService statsService;
    @Autowired
    private MatchService matchService;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private TestData data;

    private User paul;
    private User alex;
    private Card vi;
    private Card leblanc;
    private Card jinx;
    private Card starSpring;

    @BeforeEach
    void setUp() {
        paul = data.user("Paul");
        alex = data.user("Alex");
        vi = data.card("test-001", "Vi - Piltover Enforcer", Card.TYPE_LEGEND);
        leblanc = data.card("test-002", "LeBlanc - Deceiver", Card.TYPE_LEGEND);
        jinx = data.card("test-003", "Jinx - Loose Cannon", Card.TYPE_LEGEND);
        starSpring = data.card("test-004", "Star Spring", Card.TYPE_BATTLEFIELD);
    }

    private StatsResponse statsOf(User user) {
        entityManager.flush();
        entityManager.clear();
        return statsService.forUser(user.getId());
    }

    @Test
    void emptyHistoryGivesZeroes() {
        StatsResponse stats = statsOf(paul);

        assertEquals(new Tally(0, 0, 0, 0), stats.games());
        assertNull(stats.averageGameSeconds());
        assertEquals(List.of(), stats.opponentLegends());
    }

    @Test
    void countsResultsFromEachPlayersSeat() {
        // Paul (seat 1, Vi) beats Alex (LeBlanc) 2-1 in a Bo3, going first in games 1 and 3.
        matchService.upsert(UUID.randomUUID(), match(MatchFormat.BO3,
                List.of(userSeat(1, paul, vi), userSeat(2, alex, leblanc)),
                liveGame(1, 1, GameResult.SEAT1, 8, 3),
                liveGame(2, 2, GameResult.SEAT2, 4, 8),
                liveGame(3, 1, GameResult.SEAT1, 8, 7)), paul.getId());
        // Paul loses a Bo1 against a guest playing Jinx, going second.
        matchService.upsert(UUID.randomUUID(), match(MatchFormat.BO1,
                List.of(userSeat(1, paul, vi), guestSeat(2, "Thomas", jinx)),
                liveGame(1, 2, GameResult.SEAT2, 5, 8)), paul.getId());

        StatsResponse paulStats = statsOf(paul);
        assertEquals(new Tally(2, 1, 1, 0), paulStats.matches());
        assertEquals(new Tally(4, 2, 2, 0), paulStats.games());
        assertEquals(new Tally(2, 2, 0, 0), paulStats.goingFirst());
        assertEquals(new Tally(2, 0, 2, 0), paulStats.goingSecond());

        var vsLeblanc = paulStats.opponentLegends().get(0);
        assertEquals("LeBlanc - Deceiver", vsLeblanc.legend().name());
        assertEquals(new Tally(3, 2, 1, 0), vsLeblanc.all());
        assertEquals(new Tally(1, 0, 1, 0), vsLeblanc.goingSecond());
        assertEquals(new Tally(4, 2, 2, 0), paulStats.myLegends().get(0).all());

        // Points: Paul scored 8+4+8+5 and conceded 3+8+7+8, over 4 counted games.
        var scored = paulStats.pointsScored();
        assertEquals(25, scored.conquer() + scored.hold() + scored.ability());
        assertEquals(4, scored.games());
        var conceded = paulStats.pointsConceded();
        assertEquals(26, conceded.conquer() + conceded.hold() + conceded.ability());

        // The same Bo3 seen from seat 2.
        StatsResponse alexStats = statsOf(alex);
        assertEquals(new Tally(1, 0, 1, 0), alexStats.matches());
        assertEquals(new Tally(3, 1, 2, 0), alexStats.games());
        assertEquals(new Tally(1, 1, 0, 0), alexStats.goingFirst());
    }

    @Test
    void ranksBattlefieldsByLosses() {
        GamePlayerData onStarSpring = new GamePlayerData(1, null, starSpring.getId(), null, null);
        matchService.upsert(UUID.randomUUID(), match(MatchFormat.BO3,
                List.of(userSeat(1, paul, vi), userSeat(2, alex, leblanc)),
                withDetails(liveGame(1, 1, GameResult.SEAT2, 2, 8), onStarSpring),
                withDetails(liveGame(2, 1, GameResult.SEAT2, 6, 8), onStarSpring)), paul.getId());

        StatsResponse stats = statsOf(paul);

        assertEquals(1, stats.myBattlefields().size());
        assertEquals("Star Spring", stats.myBattlefields().get(0).battlefield().name());
        assertEquals(new Tally(2, 0, 2, 0), stats.myBattlefields().get(0).games());
        // Alex sees it as the opponent's battlefield.
        assertEquals(new Tally(2, 2, 0, 0), statsOf(alex).opponentBattlefields().get(0).games());
    }

    @Test
    void findsComebacksFromThreePointsBehind() {
        matchService.upsert(UUID.randomUUID(), match(MatchFormat.BO3,
                List.of(userSeat(1, paul, vi), guestSeat(2, "Thomas", jinx)),
                // Paul trails 2-5, then wins 8-5: a comeback.
                scriptedGame(1, 2, GameResult.SEAT1, "1122222" + "111111"),
                // Paul only trails 0-2: not a comeback.
                scriptedGame(2, 1, GameResult.SEAT1, "22" + "11111111")), paul.getId());

        List<Comeback> comebacks = statsOf(paul).comebacks();

        assertEquals(1, comebacks.size());
        Comeback comeback = comebacks.get(0);
        assertEquals(1, comeback.gameNumber());
        assertEquals("Thomas", comeback.opponentName());
        assertEquals(2, comeback.scoreWhenBehind());
        assertEquals(5, comeback.opponentScoreWhenBehind());
        assertEquals(8, comeback.finalScore());
        assertEquals(5, comeback.opponentFinalScore());
    }
}
