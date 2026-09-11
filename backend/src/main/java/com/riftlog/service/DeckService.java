package com.riftlog.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.riftlog.dto.DeckRequest;
import com.riftlog.dto.DeckResponse;
import com.riftlog.entity.Deck;
import com.riftlog.entity.Legend;
import com.riftlog.repository.DeckRepository;

@Service
public class DeckService {

    private final DeckRepository deckRepository;
    private final LegendService legendService;

    public DeckService(DeckRepository deckRepository, LegendService legendService) {
        this.deckRepository = deckRepository;
        this.legendService = legendService;
    }

    public List<DeckResponse> listAll() {
        return deckRepository.findAll().stream().map(this::toResponse).toList();
    }

    public DeckResponse create(DeckRequest request) {
        Legend legend = legendService.findOrCreateByName(request.legendName());
        return toResponse(findOrCreate(request.name(), legend));
    }

    public Deck findOrCreate(String deckName, Legend legend) {
        return deckRepository.findByNameIgnoreCaseAndLegendId(deckName, legend.getId())
                .orElseGet(() -> {
                    Deck deck = new Deck();
                    deck.setName(deckName);
                    deck.setLegend(legend);
                    return deckRepository.save(deck);
                });
    }

    private DeckResponse toResponse(Deck deck) {
        return new DeckResponse(deck.getId(), deck.getName(), deck.getLegend().getName());
    }
}
