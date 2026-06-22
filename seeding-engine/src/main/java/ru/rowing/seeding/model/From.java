package ru.rowing.seeding.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Ссылка на конкретный заезд этапа-источника. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record From(String stage, int heat) {
}
