package com.riftlog.dto;

public record UserResponse(
        Long id,
        String email,
        String displayName
) {
}
