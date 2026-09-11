package com.riftlog.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.riftlog.entity.Player;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByNameIgnoreCase(String name);
}
