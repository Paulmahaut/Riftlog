package com.riftlog.dto;

import jakarta.validation.constraints.NotBlank;

public record LegendRequest(
        @NotBlank String name
) {
}
