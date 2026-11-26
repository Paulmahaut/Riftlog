package com.riftlog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.riftlog.entity.GuestPlayer;

public interface GuestPlayerRepository extends JpaRepository<GuestPlayer, Long> {

    Optional<GuestPlayer> findFirstByNameIgnoreCase(String name);

    List<GuestPlayer> findAllByOrderByNameAsc();
}
