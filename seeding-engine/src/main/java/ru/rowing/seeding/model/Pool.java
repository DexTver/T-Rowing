package ru.rowing.seeding.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Пул для отбора по времени: этап-источник и признак исключения прошедших по местам. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Pool(
        String stage,
        @JsonProperty("exclude_advanced") Boolean excludeAdvanced
) {
    public boolean excludeAdvancedOrDefault() {
        return excludeAdvanced != null && excludeAdvanced;
    }
}
