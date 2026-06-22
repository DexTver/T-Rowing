package ru.rowing.seeding.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Описание целевого этапа и посева переходов к нему, разложенного по вариантам.
 * Для полуфинала задаётся {@code heatsCount}; для финала — список букв {@code finals}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StageSpec(
        String stage,
        @JsonProperty("heats_count") Integer heatsCount,
        List<String> finals,
        @JsonProperty("resolution_order") List<String> resolutionOrder,
        Map<String, List<Assignment>> variants
) {
    /** Порядок разрешения финалов: {@code resolution_order}, иначе порядок {@code finals}, иначе только A. */
    public List<String> finalResolutionOrder() {
        if (resolutionOrder != null && !resolutionOrder.isEmpty()) {
            return resolutionOrder;
        }
        if (finals != null && !finals.isEmpty()) {
            return finals;
        }
        return List.of("A");
    }

    /** Возвращает назначения для варианта; если варианта нет — пробует "default", затем первый доступный. */
    public List<Assignment> assignmentsFor(String variant) {
        if (variants == null || variants.isEmpty()) {
            return List.of();
        }
        if (variant != null && variants.containsKey(variant)) {
            return variants.get(variant);
        }
        if (variants.containsKey("default")) {
            return variants.get("default");
        }
        return variants.values().iterator().next();
    }
}
