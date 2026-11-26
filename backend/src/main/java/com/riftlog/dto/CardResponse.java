package com.riftlog.dto;

import java.util.List;

public record CardResponse(
        Long id,
        String riftboundId,
        String name,
        String baseName,
        String type,
        String setId,
        List<String> domains,
        String imageUrl,
        boolean variant
) {
}
