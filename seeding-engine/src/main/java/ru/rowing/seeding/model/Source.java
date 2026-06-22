package ru.rowing.seeding.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Селектор источника участника (раздел 8.4). Полиморфен по полю {@code type}:
 * <ul>
 *   <li>{@code place} — конкретное место конкретного заезда;</li>
 *   <li>{@code best_time} — лучшие по времени среди не прошедших по местам;</li>
 *   <li>{@code place_by_time} — сквозной отбор «X из Y-ых».</li>
 * </ul>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PlaceSource.class, name = "place"),
        @JsonSubTypes.Type(value = BestTimeSource.class, name = "best_time"),
        @JsonSubTypes.Type(value = PlaceByTimeSource.class, name = "place_by_time")
})
public sealed interface Source permits PlaceSource, BestTimeSource, PlaceByTimeSource {
    String type();
}
