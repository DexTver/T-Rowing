package ru.rowing.seeding.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Описание предварительного этапа.
 * Размеры заездов задаются либо формулой {@code sizing} ("even_desc"), либо явной таблицей {@code sizes} по N.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Prelims(
        int count,
        String sizing,
        Map<String, List<Integer>> sizes
) {
}
