package com.riftlog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.riftlog.entity.Deck;

public interface DeckRepository extends JpaRepository<Deck, Long> {
    Optional<Deck> findByNameIgnoreCaseAndLegendIdAndOwnerId(String name, Long legendId, Long ownerId);

    List<Deck> findByOwnerId(Long ownerId);
}
