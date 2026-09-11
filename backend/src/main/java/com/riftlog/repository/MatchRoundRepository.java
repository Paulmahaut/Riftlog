package com.riftlog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.riftlog.entity.MatchRound;

public interface MatchRoundRepository extends JpaRepository<MatchRound, Long> {
    List<MatchRound> findByMatchIdOrderByRoundNumberAsc(Long matchId);
}
