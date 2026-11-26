package com.riftlog.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.riftlog.dto.DeckRequest;
import com.riftlog.dto.DeckResponse;
import com.riftlog.dto.Saved;
import com.riftlog.entity.Card;
import com.riftlog.entity.Deck;
import com.riftlog.entity.DeckCard;
import com.riftlog.entity.DeckZone;
import com.riftlog.entity.User;
import com.riftlog.exception.ForbiddenException;
import com.riftlog.exception.NotFoundException;
import com.riftlog.repository.DeckRepository;

@Service
public class DeckService {

    private static final Logger log = LoggerFactory.getLogger(DeckService.class);

    private final DeckRepository deckRepository;
    private final CardService cardService;
    private final UserService userService;

    public DeckService(DeckRepository deckRepository, CardService cardService, UserService userService) {
        this.deckRepository = deckRepository;
        this.cardService = cardService;
        this.userService = userService;
    }

    /** Every deck of the group, or only one player's decks. */
    @Transactional(readOnly = true)
    public List<DeckResponse> list(Long ownerId) {
        List<Deck> decks = ownerId == null
                ? deckRepository.findAllByOrderByUpdatedAtDesc()
                : deckRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId);
        return decks.stream().map(DeckService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DeckResponse get(UUID id) {
        return toResponse(require(id));
    }

    /** Creates the deck, or replaces it when it already exists. Only its owner can change it. */
    @Transactional
    public Saved<DeckResponse> upsert(UUID id, DeckRequest request, Long currentUserId) {
        Card legend = cardService.require(request.legendCardId(), Card.TYPE_LEGEND, "Deck legend");
        Map<String, DeckCard> cards = resolveCards(request.cards());

        Deck deck = deckRepository.findById(id).orElse(null);
        boolean created = deck == null;
        Instant now = Instant.now();
        if (created) {
            User owner = userService.require(currentUserId);
            deck = new Deck();
            deck.setId(id);
            deck.setOwner(owner);
            deck.setCreatedAt(now);
        } else {
            ensureOwner(deck, currentUserId);
            // Delete the old list before inserting the new one: same (card, zone) pairs would clash otherwise.
            deck.getCards().clear();
            deckRepository.flush();
        }

        deck.setName(request.name().trim());
        deck.setLegend(legend);
        deck.setNotes(request.notes());
        deck.setUpdatedAt(now);
        for (DeckCard deckCard : cards.values()) {
            deckCard.setDeck(deck);
            deck.getCards().add(deckCard);
        }

        Deck saved = deckRepository.save(deck);
        log.info("User {} {} deck {} ({}, {} cards)", currentUserId, created ? "created" : "updated",
                saved.getId(), saved.getName(), saved.getCards().size());
        return new Saved<>(toResponse(saved), created);
    }

    @Transactional
    public void delete(UUID id, Long currentUserId) {
        Deck deck = require(id);
        ensureOwner(deck, currentUserId);
        // Matches keep their legend; the database sets their deck reference to NULL.
        deckRepository.delete(deck);
        log.info("User {} deleted deck {}", currentUserId, id);
    }

    private Deck require(UUID id) {
        return deckRepository.findById(id).orElseThrow(() -> new NotFoundException("Deck " + id + " not found"));
    }

    private static void ensureOwner(Deck deck, Long currentUserId) {
        if (!deck.getOwner().getId().equals(currentUserId)) {
            throw new ForbiddenException("Only the owner of deck " + deck.getId() + " can change it");
        }
    }

    /** Same card listed twice in the same zone → quantities are added up. */
    private Map<String, DeckCard> resolveCards(List<DeckRequest.DeckCardRequest> requested) {
        Map<String, DeckCard> cards = new LinkedHashMap<>();
        if (requested == null) {
            return cards;
        }
        for (DeckRequest.DeckCardRequest entry : requested) {
            Card card = cardService.require(entry.cardId(), null, "Deck card");
            DeckZone zone = entry.zone();
            cards.merge(card.getId() + "|" + zone, newDeckCard(card, zone, entry.quantity()), (existing, added) -> {
                existing.setQuantity(existing.getQuantity() + added.getQuantity());
                return existing;
            });
        }
        return cards;
    }

    private static DeckCard newDeckCard(Card card, DeckZone zone, int quantity) {
        DeckCard deckCard = new DeckCard();
        deckCard.setCard(card);
        deckCard.setZone(zone);
        deckCard.setQuantity(quantity);
        return deckCard;
    }

    static DeckResponse toResponse(Deck deck) {
        List<DeckResponse.DeckCardResponse> cards = deck.getCards().stream()
                .map(entry -> new DeckResponse.DeckCardResponse(CardService.toRef(entry.getCard()), entry.getZone(),
                        entry.getQuantity()))
                .toList();
        return new DeckResponse(deck.getId(), deck.getOwner().getId(), deck.getOwner().getDisplayName(), deck.getName(),
                CardService.toRef(deck.getLegend()), deck.getNotes(), deck.getCreatedAt(), deck.getUpdatedAt(), cards);
    }
}
