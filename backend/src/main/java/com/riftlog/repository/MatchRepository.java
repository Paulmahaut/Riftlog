package com.riftlog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.riftlog.entity.Match;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByOpponentId(Long opponentId);

    List<Match> findByMyDeckId(Long deckId);
}
