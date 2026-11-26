package com.riftlog.service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.riftlog.dto.CardRef;
import com.riftlog.dto.CardResponse;
import com.riftlog.entity.Card;
import com.riftlog.exception.BadRequestException;
import com.riftlog.repository.CardRepository;

@Service
public class CardService {

    private final CardRepository cardRepository;

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    /**
     * @param type            e.g. "legend", "battlefield" (case-insensitive), or null for all types
     * @param query           part of the card name, or null
     * @param includeVariants false = one printing per card, which is what pickers want
     */
    @Transactional(readOnly = true)
    public List<CardResponse> search(String type, String query, boolean includeVariants) {
        String normalizedType = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return cardRepository.search(normalizedType, normalizedQuery, includeVariants).stream()
                .map(CardService::toResponse)
                .toList();
    }

    /** Loads a card referenced by a request, checking its type when one is expected. */
    public Card require(Long id, String expectedType, String role) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(role + ": unknown card " + id));
        if (expectedType != null && !card.isType(expectedType)) {
            throw new BadRequestException(role + " must be a " + expectedType.toLowerCase(Locale.ROOT)
                    + " card, but " + card.getName() + " is a " + card.getCardType().toLowerCase(Locale.ROOT));
        }
        return card;
    }

    public static CardRef toRef(Card card) {
        return card == null ? null : new CardRef(card.getId(), card.getBaseName(), card.getImageUrl());
    }

    public static CardResponse toResponse(Card card) {
        List<String> domains = card.getDomains() == null ? List.of() : Arrays.asList(card.getDomains().split(","));
        return new CardResponse(card.getId(), card.getRiftboundId(), card.getName(), card.getBaseName(),
                card.getCardType(), card.getSetId(), domains, card.getImageUrl(), card.isVariant());
    }
}
