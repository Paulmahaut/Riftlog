package com.riftlog.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.riftlog.dto.CardImportResult;
import com.riftlog.dto.CardResponse;
import com.riftlog.service.CardImportService;
import com.riftlog.service.CardService;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;
    private final CardImportService cardImportService;

    public CardController(CardService cardService, CardImportService cardImportService) {
        this.cardService = cardService;
        this.cardImportService = cardImportService;
    }

    /** e.g. GET /api/cards?type=legend, GET /api/cards?type=battlefield&q=altar */
    @GetMapping
    public List<CardResponse> search(@RequestParam(required = false) String type,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean includeVariants) {
        return cardService.search(type, q, includeVariants);
    }

    /** Refreshes the card list from Riftcodex, for example after a new set is released. */
    @PostMapping("/import")
    public CardImportResult importCards() {
        return cardImportService.importAll();
    }
}
