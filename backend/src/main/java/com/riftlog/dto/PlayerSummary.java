package com.riftlog.dto;

/** A registered user as other members of the group see them. */
public record PlayerSummary(
        Long id,
        String displayName
) {
}
