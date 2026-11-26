package com.riftlog.service;

import static com.riftlog.support.TestData.guestSeat;
import static com.riftlog.support.TestData.liveGame;
import static com.riftlog.support.TestData.match;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.riftlog.dto.DeckRequest;
import com.riftlog.dto.DeckRequest.DeckCardRequest;
import com.riftlog.dto.DeckResponse;
import com.riftlog.dto.MatchRequest.PlayerSlot;
import com.riftlog.dto.MatchResponse;
import com.riftlog.dto.Saved;
import com.riftlog.entity.Card;
import com.riftlog.entity.DeckZone;
import com.riftlog.entity.GameResult;
import com.riftlog.entity.MatchFormat;
import com.riftlog.entity.User;
import com.riftlog.exception.BadRequestException;
import com.riftlog.exception.ForbiddenException;
import com.riftlog.exception.InvalidMatchException;
import com.riftlog.support.TestData;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class DeckServiceTest {

    @Autowired
    private DeckService deckService;
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
    private Card unit;

    @BeforeEach
    void setUp() {
        paul = data.user("Paul");
        alex = data.user("Alex");
        vi = data.card("test-101", "Vi - Piltover Enforcer", Card.TYPE_LEGEND);
        leblanc = data.card("test-102", "LeBlanc - Deceiver", Card.TYPE_LEGEND);
        unit = data.card("test-103", "Blazing Scorcher", "UNIT");
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void createsADeckAndAddsUpDuplicateLines() {
        UUID id = UUID.randomUUID();
        DeckRequest request = new DeckRequest("Vi aggro", vi.getId(), null, List.of(
                new DeckCardRequest(unit.getId(), DeckZone.MAIN, 2),
                new DeckCardRequest(unit.getId(), DeckZone.MAIN, 1),
                new DeckCardRequest(unit.getId(), DeckZone.SIDE, 1)));

        Saved<DeckResponse> saved = deckService.upsert(id, request, paul.getId());
        flushAndClear();
        DeckResponse deck = deckService.get(id);

        assertTrue(saved.created());
        assertEquals("Vi - Piltover Enforcer", deck.legend().name());
        assertEquals(2, deck.cards().size());
        assertEquals(3, deck.cards().stream().filter(c -> c.zone() == DeckZone.MAIN).findFirst().orElseThrow().quantity());
    }

    @Test
    void updatingReplacesTheCardList() {
        UUID id = UUID.randomUUID();
        deckService.upsert(id, new DeckRequest("Vi aggro", vi.getId(), null,
                List.of(new DeckCardRequest(unit.getId(), DeckZone.MAIN, 3))), paul.getId());
        flushAndClear();

        deckService.upsert(id, new DeckRequest("Vi aggro v2", vi.getId(), "Less greedy",
                List.of(new DeckCardRequest(unit.getId(), DeckZone.MAIN, 2))), paul.getId());
        flushAndClear();
        DeckResponse deck = deckService.get(id);

        assertEquals("Vi aggro v2", deck.name());
        assertEquals(1, deck.cards().size());
        assertEquals(2, deck.cards().get(0).quantity());
    }

    @Test
    void onlyTheOwnerCanChangeOrDeleteADeck() {
        UUID id = UUID.randomUUID();
        DeckRequest request = new DeckRequest("Vi aggro", vi.getId(), null, null);
        deckService.upsert(id, request, paul.getId());
        flushAndClear();

        assertThrows(ForbiddenException.class, () -> deckService.upsert(id, request, alex.getId()));
        assertThrows(ForbiddenException.class, () -> deckService.delete(id, alex.getId()));
    }

    @Test
    void theLegendMustBeALegendCard() {
        DeckRequest request = new DeckRequest("Broken", unit.getId(), null, null);

        assertThrows(BadRequestException.class, () -> deckService.upsert(UUID.randomUUID(), request, paul.getId()));
    }

    @Test
    void deletingADeckKeepsItsMatches() {
        UUID deckId = UUID.randomUUID();
        deckService.upsert(deckId, new DeckRequest("Vi aggro", vi.getId(), null, null), paul.getId());
        UUID matchId = UUID.randomUUID();
        List<PlayerSlot> players = List.of(new PlayerSlot(1, paul.getId(), null, null, deckId, null),
                guestSeat(2, "Thomas", leblanc));
        matchService.upsert(matchId, match(MatchFormat.BO1, players, liveGame(1, 1, GameResult.SEAT1, 8, 3)),
                paul.getId());
        flushAndClear();

        deckService.delete(deckId, paul.getId());
        flushAndClear();
        MatchResponse match = matchService.get(matchId);

        assertNull(match.players().get(0).deckId());
        assertEquals("Vi - Piltover Enforcer", match.players().get(0).legend().name());
    }

    @Test
    void aMatchCannotUseSomeoneElsesDeck() {
        UUID alexDeck = UUID.randomUUID();
        deckService.upsert(alexDeck, new DeckRequest("Alex's Vi", vi.getId(), null, null), alex.getId());
        List<PlayerSlot> players = List.of(new PlayerSlot(1, paul.getId(), null, null, alexDeck, null),
                guestSeat(2, "Thomas", leblanc));

        assertThrows(InvalidMatchException.class, () -> matchService.upsert(UUID.randomUUID(),
                match(MatchFormat.BO1, players, liveGame(1, 1, GameResult.SEAT1, 8, 3)), paul.getId()));
    }

    @Test
    void unknownDeckIsRejected() {
        List<PlayerSlot> players = List.of(new PlayerSlot(1, paul.getId(), null, null, UUID.randomUUID(), null),
                guestSeat(2, "Thomas", leblanc));

        assertThrows(BadRequestException.class, () -> matchService.upsert(UUID.randomUUID(),
                match(MatchFormat.BO1, players, liveGame(1, 1, GameResult.SEAT1, 8, 3)), paul.getId()));
    }
}
