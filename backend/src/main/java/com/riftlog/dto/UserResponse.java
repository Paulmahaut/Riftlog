package com.riftlog.dto;

import com.riftlog.entity.MatchFormat;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        MatchFormat defaultFormat
) {
}
