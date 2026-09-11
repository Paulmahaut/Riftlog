package com.riftlog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.riftlog.entity.Player;
import com.riftlog.repository.PlayerRepository;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private PlayerService playerService;

    @Test
    void findOrCreateByName_returnsExistingPlayerWithoutSaving() {
        Player existing = new Player();
        existing.setId(1L);
        existing.setName("Julien");
        when(playerRepository.findByNameIgnoreCase("julien")).thenReturn(Optional.of(existing));

        Player result = playerService.findOrCreateByName("julien");

        assertEquals(existing, result);
        verify(playerRepository, never()).save(any());
    }

    @Test
    void findOrCreateByName_createsNewPlayerWhenNotFound() {
        when(playerRepository.findByNameIgnoreCase("Marie")).thenReturn(Optional.empty());
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Player result = playerService.findOrCreateByName("Marie");

        assertEquals("Marie", result.getName());
        verify(playerRepository).save(any(Player.class));
    }
}
