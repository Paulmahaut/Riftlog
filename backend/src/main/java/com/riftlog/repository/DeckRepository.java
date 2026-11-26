package com.riftlog.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.riftlog.entity.Deck;

public interface DeckRepository extends JpaRepository<Deck, UUID> {

    List<Deck> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);

    List<Deck> findAllByOrderByUpdatedAtDesc();
}
