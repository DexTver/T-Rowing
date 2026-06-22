package ru.rowing.seeding.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Одно назначение: «источник → целевая дорожка целевого заезда» (раздел 8.3). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Assignment(Target target, Source source) {
}
