package ru.rowing.seeding.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** «по вр.» — {@code rank}-й по времени среди не прошедших по местам в пуле {@code pool}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BestTimeSource(String type, int rank, Pool pool) implements Source {
}
