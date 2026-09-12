package com.riftlog.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.riftlog.dto.DeckRequest;
import com.riftlog.dto.DeckResponse;
import com.riftlog.entity.Deck;
import com.riftlog.entity.Legend;
import com.riftlog.entity.User;
import com.riftlog.exception.NotFoundException;
import com.riftlog.repository.DeckRepository;
import com.riftlog.repository.UserRepository;

@Service
public class DeckService {

    private final DeckRepository deckRepository;
    private final LegendService legendService;
    private final UserRepository userRepository;

    public DeckService(DeckRepository deckRepository, LegendService legendService, UserRepository userRepository) {
        this.deckRepository = deckRepository;
        this.legendService = legendService;
        this.userRepository = userRepository;
    }

    public List<DeckResponse> listAll(Long ownerId) {
        return deckRepository.findByOwnerId(ownerId).stream().map(this::toResponse).toList();
    }

    public DeckResponse create(DeckRequest request, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("User " + ownerId + " not found"));
        Legend legend = legendService.findOrCreateByName(request.legendName());
        return toResponse(findOrCreate(request.name(), legend, owner));
    }

    public Deck findOrCreate(String deckName, Legend legend, User owner) {
        return deckRepository.findByNameIgnoreCaseAndLegendIdAndOwnerId(deckName, legend.getId(), owner.getId())
                .orElseGet(() -> {
                    Deck deck = new Deck();
                    deck.setName(deckName);
                    deck.setLegend(legend);
                    deck.setOwner(owner);
                    return deckRepository.save(deck);
                });
    }

    private DeckResponse toResponse(Deck deck) {
        return new DeckResponse(deck.getId(), deck.getName(), deck.getLegend().getName());
    }
}
