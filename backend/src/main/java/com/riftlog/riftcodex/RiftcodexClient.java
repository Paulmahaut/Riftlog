package com.riftlog.riftcodex;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.riftlog.exception.CardImportException;

/**
 * Reads the public Riftcodex API (unofficial fan project, no key needed).
 * Riftlog copies the cards into its own database and never calls this at request time.
 */
@Component
public class RiftcodexClient {

    private static final Logger log = LoggerFactory.getLogger(RiftcodexClient.class);
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 200;

    private final RestClient restClient;

    public RiftcodexClient(@Value("${app.riftcodex.base-url:https://api.riftcodex.com}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "Riftlog card importer")
                .build();
    }

    public List<RiftcodexCard> fetchAllCards() {
        List<RiftcodexCard> cards = new ArrayList<>();
        int page = 1;
        int pages;
        do {
            RiftcodexPage body = fetchPage(page);
            if (body == null || body.items() == null) {
                throw new CardImportException("Riftcodex returned an empty page " + page, null);
            }
            cards.addAll(body.items());
            pages = body.pages();
            page++;
        } while (page <= pages && page <= MAX_PAGES);

        log.info("Fetched {} cards from Riftcodex ({} pages)", cards.size(), pages);
        return cards;
    }

    private RiftcodexPage fetchPage(int page) {
        try {
            return restClient.get()
                    .uri(uri -> uri.path("/cards").queryParam("page", page).queryParam("size", PAGE_SIZE).build())
                    .retrieve()
                    .body(RiftcodexPage.class);
        } catch (RestClientException ex) {
            throw new CardImportException("Could not read page " + page + " from Riftcodex: " + ex.getMessage(), ex);
        }
    }
}
