package com.riftlog.dto;

/** Compact card reference embedded in decks and matches. */
public record CardRef(
        Long id,
        String name,
        String imageUrl
) {
}
