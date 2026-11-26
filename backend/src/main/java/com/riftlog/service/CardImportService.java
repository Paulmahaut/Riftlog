package com.riftlog.service;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.riftlog.dto.CardImportResult;
import com.riftlog.entity.Card;
import com.riftlog.repository.CardRepository;
import com.riftlog.riftcodex.RiftcodexCard;
import com.riftlog.riftcodex.RiftcodexClient;

/**
 * Copies the Riftcodex card list into the cards table.
 * Safe to run again: cards are matched on their Riftbound id and updated in place,
 * so ids referenced by decks and matches never change.
 */
@Service
public class CardImportService {

    private static final Logger log = LoggerFactory.getLogger(CardImportService.class);
    /** A trailing "(Signature)", "(Alternate Art)", "(274)"... */
    private static final Pattern TRAILING_TAG = Pattern.compile("\\s*\\([^)]*\\)\\s*$");

    private final RiftcodexClient riftcodexClient;
    private final CardRepository cardRepository;
    private final TransactionTemplate transactionTemplate;

    public CardImportService(RiftcodexClient riftcodexClient, CardRepository cardRepository,
            TransactionTemplate transactionTemplate) {
        this.riftcodexClient = riftcodexClient;
        this.cardRepository = cardRepository;
        this.transactionTemplate = transactionTemplate;
    }

    /** Fetches outside the transaction so no DB connection is held during the HTTP calls. */
    public CardImportResult importAll() {
        List<RiftcodexCard> fetched = riftcodexClient.fetchAllCards();
        return transactionTemplate.execute(status -> upsert(fetched));
    }

    CardImportResult upsert(List<RiftcodexCard> fetched) {
        Map<String, Card> byRiftcodexId = cardRepository.findAll().stream()
                .collect(Collectors.toMap(Card::getRiftcodexId, Function.identity()));
        Instant now = Instant.now();
        int created = 0;
        int updated = 0;

        for (RiftcodexCard source : fetched) {
            if (!isUsable(source)) {
                log.warn("Skipping unusable Riftcodex card {}", source.id());
                continue;
            }
            Card card = byRiftcodexId.get(source.id());
            if (card == null) {
                card = new Card();
                card.setRiftcodexId(source.id());
                byRiftcodexId.put(source.id(), card);
                created++;
            } else {
                updated++;
            }
            apply(source, card, now);
        }

        assignCanonical(byRiftcodexId.values());
        cardRepository.saveAll(byRiftcodexId.values());

        long legends = countCanonical(byRiftcodexId.values(), Card.TYPE_LEGEND);
        long battlefields = countCanonical(byRiftcodexId.values(), Card.TYPE_BATTLEFIELD);
        log.info("Card import done: {} fetched, {} created, {} updated, {} legends, {} battlefields",
                fetched.size(), created, updated, legends, battlefields);
        return new CardImportResult(fetched.size(), created, updated, legends, battlefields);
    }

    private static boolean isUsable(RiftcodexCard source) {
        return source.id() != null && !source.id().isBlank()
                && source.riftboundId() != null && !source.riftboundId().isBlank()
                && source.name() != null && !source.name().isBlank()
                && source.classification() != null && source.classification().type() != null
                && source.setInfo() != null && source.setInfo().setId() != null;
    }

    private static void apply(RiftcodexCard source, Card card, Instant now) {
        String cardType = source.classification().type().trim().toUpperCase(Locale.ROOT);
        String name = displayName(source.name(), cardType, source.tags());
        String baseName = baseName(name);
        RiftcodexCard.Metadata metadata = source.metadata();
        boolean flaggedVariant = metadata != null && (isTrue(metadata.alternateArt())
                || isTrue(metadata.overnumbered()) || isTrue(metadata.signature()));
        List<String> domains = source.classification().domain();

        card.setRiftboundId(source.riftboundId().trim());
        card.setName(name);
        card.setBaseName(baseName);
        card.setCardType(cardType);
        card.setSupertype(source.classification().supertype());
        card.setSetId(source.setInfo().setId().trim().toUpperCase(Locale.ROOT));
        card.setDomains(domains == null || domains.isEmpty() ? null : String.join(",", domains));
        card.setImageUrl(source.media() == null ? null : source.media().imageUrl());
        card.setVariant(flaggedVariant || !baseName.equals(card.getName()));
        card.setUpdatedAt(now);
    }

    /**
     * Legends are named "Champion - Title", but some Riftcodex entries (VEN set) only carry the title,
     * with the champion in their tags: "Defender of Tomorrow" + ["Jayce"] → "Jayce - Defender of Tomorrow".
     * The champion is the last tag (["Yordle", "Kennen"] → Kennen). Some entries also put the tags in the
     * name ("Yordle, Kennen - Heart of the Tempest"), so only the last comma-separated champion is kept.
     */
    static String displayName(String name, String cardType, List<String> tags) {
        String trimmed = name.trim();
        if (!Card.TYPE_LEGEND.equals(cardType)) {
            return trimmed;
        }
        int separator = trimmed.indexOf(" - ");
        if (separator > 0) {
            String champion = trimmed.substring(0, separator);
            return champion.substring(champion.lastIndexOf(',') + 1).trim() + trimmed.substring(separator);
        }
        if (tags == null || tags.isEmpty()) {
            return trimmed;
        }
        String champion = tags.get(tags.size() - 1);
        return champion == null || champion.isBlank() ? trimmed : champion.trim() + " - " + trimmed;
    }

    /** "Vi - Piltover Enforcer (Signature)" → "Vi - Piltover Enforcer". */
    static String baseName(String name) {
        String current = name.trim();
        String previous;
        do {
            previous = current;
            current = TRAILING_TAG.matcher(current).replaceFirst("");
        } while (!current.equals(previous) && !current.isEmpty());
        return current.isEmpty() ? name.trim() : current;
    }

    /**
     * Marks one printing per (type, base name) as canonical: the one pickers show.
     * An existing non-variant canonical printing is kept so the choice stays stable across imports;
     * otherwise the first non-variant printing by Riftbound id wins.
     */
    static void assignCanonical(Collection<Card> cards) {
        Map<String, List<Card>> groups = cards.stream()
                .collect(Collectors.groupingBy(card -> card.getCardType() + "|" + card.getBaseName().toLowerCase(Locale.ROOT)));

        for (List<Card> group : groups.values()) {
            Card keep = group.stream()
                    .filter(card -> card.isCanonical() && !card.isVariant())
                    .findFirst()
                    .orElseGet(() -> group.stream()
                            .min(Comparator.comparing(Card::isVariant)
                                    .thenComparing(Card::getRiftboundId)
                                    .thenComparing(Card::getRiftcodexId))
                            .orElseThrow());
            group.forEach(card -> card.setCanonical(card == keep));
        }
    }

    private static long countCanonical(Collection<Card> cards, String type) {
        return cards.stream().filter(card -> card.isCanonical() && card.isType(type)).count();
    }

    private static boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}
