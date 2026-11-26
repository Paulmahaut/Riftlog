package com.riftlog.riftcodex;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** The subset of a Riftcodex card that Riftlog stores. See https://riftcodex.com/docs/endpoints/cards */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RiftcodexCard(
        String id,
        String name,
        @JsonProperty("riftbound_id") String riftboundId,
        Classification classification,
        @JsonProperty("set") SetInfo setInfo,
        Media media,
        Metadata metadata,
        List<String> tags
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Classification(String type, String supertype, String rarity, List<String> domain) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SetInfo(@JsonProperty("set_id") String setId, String label) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Media(@JsonProperty("image_url") String imageUrl) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Metadata(
            @JsonProperty("alternate_art") Boolean alternateArt,
            Boolean overnumbered,
            Boolean signature
    ) {
    }
}
