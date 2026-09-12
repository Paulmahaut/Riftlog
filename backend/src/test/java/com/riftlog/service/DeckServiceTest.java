package com.riftlog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.riftlog.dto.DeckRequest;
import com.riftlog.dto.DeckResponse;
import com.riftlog.entity.Deck;
import com.riftlog.entity.Legend;
import com.riftlog.entity.User;
import com.riftlog.repository.DeckRepository;
import com.riftlog.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

    @Mock
    private DeckRepository deckRepository;
    @Mock
    private LegendService legendService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DeckService deckService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setEmail("paul@example.com");
        owner.setDisplayName("Paul");
    }

    @Test
    void findOrCreate_returnsExistingDeckWithoutSaving() {
        Legend legend = new Legend();
        legend.setId(1L);
        legend.setName("Ashe");
        Deck existing = new Deck();
        existing.setId(1L);
        existing.setName("Ashe Aggro");
        existing.setLegend(legend);
        existing.setOwner(owner);
        when(deckRepository.findByNameIgnoreCaseAndLegendIdAndOwnerId("Ashe Aggro", 1L, 1L)).thenReturn(Optional.of(existing));

        Deck result = deckService.findOrCreate("Ashe Aggro", legend, owner);

        assertEquals(existing, result);
        verify(deckRepository, never()).save(any());
    }

    @Test
    void findOrCreate_createsNewDeckAttachedToLegendAndOwner() {
        Legend legend = new Legend();
        legend.setId(2L);
        legend.setName("Viktor");
        when(deckRepository.findByNameIgnoreCaseAndLegendIdAndOwnerId("Viktor Control", 2L, 1L)).thenReturn(Optional.empty());
        when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Deck result = deckService.findOrCreate("Viktor Control", legend, owner);

        assertEquals("Viktor Control", result.getName());
        assertEquals(legend, result.getLegend());
        assertEquals(owner, result.getOwner());
    }

    @Test
    void create_resolvesOwnerAndLegendBeforeFindingOrCreatingDeck() {
        Legend legend = new Legend();
        legend.setId(3L);
        legend.setName("Zoe");
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(legendService.findOrCreateByName("Zoe")).thenReturn(legend);
        when(deckRepository.findByNameIgnoreCaseAndLegendIdAndOwnerId("Zoe Tempo", 3L, 1L)).thenReturn(Optional.empty());
        when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeckResponse response = deckService.create(new DeckRequest("Zoe Tempo", "Zoe"), 1L);

        assertEquals("Zoe Tempo", response.name());
        assertEquals("Zoe", response.legendName());
        verify(legendService).findOrCreateByName("Zoe");
    }
}
