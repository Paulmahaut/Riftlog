package com.riftlog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import com.riftlog.dto.CardImportResult;
import com.riftlog.entity.Card;
import com.riftlog.repository.CardRepository;
import com.riftlog.riftcodex.RiftcodexCard;
import com.riftlog.riftcodex.RiftcodexClient;

@ExtendWith(MockitoExtension.class)
class CardImportServiceTest {

    @Mock
    private RiftcodexClient riftcodexClient;
    @Mock
    private CardRepository cardRepository;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private CardImportService cardImportService;

    private static RiftcodexCard card(String riftcodexId, String riftboundId, String name, String type,
            boolean signature, List<String> tags) {
        return new RiftcodexCard(riftcodexId, name, riftboundId,
                new RiftcodexCard.Classification(type, null, "Epic", List.of("Fury", "Body")),
                new RiftcodexCard.SetInfo("unl", "Unleashed"),
                new RiftcodexCard.Media("https://example.test/" + riftboundId + ".png"),
                new RiftcodexCard.Metadata(false, false, signature),
                tags);
    }

    private static RiftcodexCard card(String riftboundId, String name, String type, boolean signature) {
        return card("rc-" + riftboundId, riftboundId, name, type, signature, List.of());
    }

    private static final List<RiftcodexCard> FETCHED = List.of(
            card("unl-229*-219", "Vi - Piltover Enforcer (Signature)", "Legend", true),
            card("unl-187-219", "Vi - Piltover Enforcer", "Legend", false),
            card("unl-215-219", "Star Spring", "Battlefield", false),
            card("ogn-001-298", "Blazing Scorcher", "Unit", false));

    /** Saved cards, keyed by Riftcodex id. */
    @SuppressWarnings("unchecked")
    private Map<String, Card> savedCards() {
        ArgumentCaptor<Collection<Card>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(cardRepository).saveAll(captor.capture());
        return captor.getValue().stream().collect(Collectors.toMap(Card::getRiftcodexId, Function.identity()));
    }

    @Test
    void baseName_dropsTrailingTags() {
        assertEquals("Vi - Piltover Enforcer", CardImportService.baseName("Vi - Piltover Enforcer (Signature)"));
        assertEquals("Jinx - Loose Cannon", CardImportService.baseName("Jinx - Loose Cannon (Alternate Art) (274)"));
        assertEquals("Star Spring", CardImportService.baseName("  Star Spring "));
        assertEquals("(Weird)", CardImportService.baseName("(Weird)"));
    }

    @Test
    void firstImport_createsCardsAndPicksTheNonVariantPrintingAsCanonical() {
        when(cardRepository.findAll()).thenReturn(List.of());

        CardImportResult result = cardImportService.upsert(FETCHED);
        Map<String, Card> saved = savedCards();

        assertEquals(4, result.created());
        assertEquals(1, result.legends());
        assertEquals(1, result.battlefields());

        Card signature = saved.get("rc-unl-229*-219");
        Card base = saved.get("rc-unl-187-219");
        assertTrue(signature.isVariant());
        assertFalse(signature.isCanonical());
        assertTrue(base.isCanonical());
        assertEquals("LEGEND", base.getCardType());
        assertEquals("UNL", base.getSetId());
        assertEquals("Fury,Body", base.getDomains());
        assertEquals("Vi - Piltover Enforcer", signature.getBaseName());
    }

    @Test
    void secondImport_updatesExistingRowsInPlace() {
        Card existing = new Card();
        existing.setId(42L);
        existing.setRiftcodexId("rc-unl-215-219");
        existing.setRiftboundId("unl-215-219");
        existing.setName("Star Sprng");
        existing.setBaseName("Star Sprng");
        existing.setCardType("BATTLEFIELD");
        existing.setCanonical(true);
        when(cardRepository.findAll()).thenReturn(new ArrayList<>(List.of(existing)));

        CardImportResult result = cardImportService.upsert(FETCHED);
        Map<String, Card> saved = savedCards();

        assertEquals(3, result.created());
        assertEquals(1, result.updated());
        assertSame(existing, saved.get("rc-unl-215-219"));
        assertEquals(42L, saved.get("rc-unl-215-219").getId());
        assertEquals("Star Spring", existing.getName());
    }

    @Test
    void printingsSharingARiftboundIdAreKeptApart() {
        when(cardRepository.findAll()).thenReturn(List.of());
        List<RiftcodexCard> fetched = List.of(
                card("a1", "opp-259-298", "Yasuo - Unforgiven", "Legend", false, List.of("Yasuo")),
                card("a2", "opp-259-298", "Yasuo - Unforgiven (Metal)", "Legend", false, List.of("Yasuo")));

        CardImportResult result = cardImportService.upsert(fetched);
        Map<String, Card> saved = savedCards();

        assertEquals(2, result.created());
        assertEquals(1, result.legends());
        assertTrue(saved.get("a1").isCanonical());
        assertTrue(saved.get("a2").isVariant());
    }

    @Test
    void legendWithoutChampionGetsItFromItsTags() {
        when(cardRepository.findAll()).thenReturn(List.of());
        List<RiftcodexCard> fetched = List.of(
                card("b1", "ven-194-166", "Defender of Tomorrow", "Legend", false, List.of("Jayce")),
                card("b2", "ven-155-166", "Heart of the Tempest", "Legend", false, List.of("Yordle", "Kennen")),
                card("b3", "ven-100-166", "Defender of Tomorrow", "Spell", false, List.of("Jayce")),
                card("b4", "ven-197-166", "Yordle, Kennen - Heart of the Tempest (Overnumbered)", "Legend", false,
                        List.of("Yordle", "Kennen")));

        CardImportResult result = cardImportService.upsert(fetched);
        Map<String, Card> saved = savedCards();

        assertEquals("Jayce - Defender of Tomorrow", saved.get("b1").getName());
        assertEquals("Kennen - Heart of the Tempest", saved.get("b2").getBaseName());
        assertEquals("Defender of Tomorrow", saved.get("b3").getName());
        assertEquals("Kennen - Heart of the Tempest", saved.get("b4").getBaseName());
        assertEquals(2, result.legends());
    }

    @Test
    void importAll_fetchesThenSavesInATransaction() {
        when(riftcodexClient.fetchAllCards()).thenReturn(FETCHED);
        when(transactionTemplate.execute(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        when(cardRepository.findAll()).thenReturn(List.of());

        CardImportResult result = cardImportService.importAll();

        assertEquals(4, result.fetched());
        verify(cardRepository).saveAll(anyCollection());
    }
}
