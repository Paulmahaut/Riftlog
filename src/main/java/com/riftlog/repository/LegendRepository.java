package com.riftlog.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.riftlog.entity.Legend;

public interface LegendRepository extends JpaRepository<Legend, Long> {
    Optional<Legend> findByNameIgnoreCase(String name);
}
