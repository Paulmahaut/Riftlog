package com.riftlog.dto;

/** Result of an idempotent PUT: the saved resource, and whether it was created (201) or replaced (200). */
public record Saved<T>(T value, boolean created) {
}
