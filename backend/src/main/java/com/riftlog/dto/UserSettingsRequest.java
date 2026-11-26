package com.riftlog.dto;

import com.riftlog.entity.MatchFormat;

import jakarta.validation.constraints.NotNull;

public record UserSettingsRequest(
        @NotNull MatchFormat defaultFormat
) {
}
