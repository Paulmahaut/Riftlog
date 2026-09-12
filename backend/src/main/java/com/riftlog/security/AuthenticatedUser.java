package com.riftlog.security;

public record AuthenticatedUser(
        Long id,
        String email,
        String displayName
) {
}
