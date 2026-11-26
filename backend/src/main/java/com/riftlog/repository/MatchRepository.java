package com.riftlog.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.riftlog.entity.Match;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    List<Match> findAllByOrderByPlayedAtDesc();

    @Query("""
            select m from Match m
            where exists (select 1 from MatchPlayer p where p.match = m and p.user.id = :userId)
            order by m.playedAt desc
            """)
    List<Match> findByParticipant(@Param("userId") Long userId);
}
