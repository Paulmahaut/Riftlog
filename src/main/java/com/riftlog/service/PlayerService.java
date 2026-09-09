package com.riftlog.service;

import org.springframework.stereotype.Service;

import com.riftlog.entity.Player;
import com.riftlog.repository.PlayerRepository;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public Player findOrCreateByName(String name) {
        return playerRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    Player player = new Player();
                    player.setName(name);
                    return playerRepository.save(player);
                });
    }
}
