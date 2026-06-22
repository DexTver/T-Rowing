package ru.rowing.seeding.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** «3 из 3-их» — среди всех {@code place}-х мест пула {@code pool}, ранжированных по времени, {@code rank}-й. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlaceByTimeSource(String type, int place, int rank, Pool pool) implements Source {
}
