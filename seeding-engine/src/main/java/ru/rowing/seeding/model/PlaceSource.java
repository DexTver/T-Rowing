package ru.rowing.seeding.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** «4/1пр» — конкретное место {@code place} конкретного заезда {@code from}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlaceSource(String type, int place, From from) implements Source {
}
