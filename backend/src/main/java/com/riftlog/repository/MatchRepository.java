package com.riftlog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.riftlog.entity.Match;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByOwnerId(Long ownerId);

    List<Match> findByOwnerIdAndOpponentId(Long ownerId, Long opponentId);

    List<Match> findByOwnerIdAndMyDeckId(Long ownerId, Long deckId);

    Optional<Match> findByIdAndOwnerId(Long id, Long ownerId);
}
