package ru.rowing.seeding.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Один файл сетки = один план (раздел 8.3 ТЗ).
 * Описывает число предварительных заездов, размеры заездов и посев каждого перехода между этапами.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Plan(
        String plan,
        Participants participants,
        int lanes,
        Prelims prelims,
        List<StageSpec> stages
) {
    /** Находит описание этапа по имени ("semifinal" / "final"); null если нет. */
    public StageSpec stage(String name) {
        if (stages == null) {
            return null;
        }
        return stages.stream().filter(s -> name.equals(s.stage())).findFirst().orElse(null);
    }
}
