package com.riftlog.riftcodex;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RiftcodexPage(
        List<RiftcodexCard> items,
        int total,
        int page,
        int size,
        int pages
) {
}
