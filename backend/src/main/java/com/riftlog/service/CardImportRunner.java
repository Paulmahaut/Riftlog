package com.riftlog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.riftlog.repository.CardRepository;

/**
 * Fills the cards table on the first start of a fresh database.
 * A failure (Riftcodex down, no network) never stops the API: run POST /api/cards/import later.
 */
@Component
@ConditionalOnProperty(name = "app.cards.import-on-startup", havingValue = "true")
public class CardImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CardImportRunner.class);

    private final CardRepository cardRepository;
    private final CardImportService cardImportService;

    public CardImportRunner(CardRepository cardRepository, CardImportService cardImportService) {
        this.cardRepository = cardRepository;
        this.cardImportService = cardImportService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (cardRepository.count() > 0) {
            return;
        }
        log.info("Cards table is empty, importing from Riftcodex");
        try {
            cardImportService.importAll();
        } catch (RuntimeException ex) {
            log.warn("Card import at startup failed, retry with POST /api/cards/import: {}", ex.getMessage());
        }
    }
}
