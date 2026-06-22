package ru.rowing.seeding.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Цель назначения. Либо заезд полуфинала ({@code heat} + {@code lane}),
 * либо финал ({@code finalLetter} + {@code lane}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Target(
        Integer heat,
        @JsonProperty("final") String finalLetter,
        int lane
) {
    public boolean isFinal() {
        return finalLetter != null;
    }
}
