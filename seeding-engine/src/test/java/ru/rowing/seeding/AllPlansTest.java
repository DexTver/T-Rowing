package ru.rowing.seeding;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.rowing.seeding.model.Plan;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Структурная проверка всех поставленных файлов сеток A–N (инварианты 8.7). */
class AllPlansTest {

    private static final String[] LETTERS = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N"
    };

    static List<Plan> loadAll() {
        List<Plan> plans = new ArrayList<>();
        for (String letter : LETTERS) {
            plans.add(PlanRepository.fromClasspath("/seeding/plan_" + letter + ".json"));
        }
        return plans;
    }

    @ParameterizedTest(name = "план {0}")
    @ValueSource(strings = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N"})
    void everyPlanSatisfiesInvariants(String letter) {
        Plan plan = PlanRepository.fromClasspath("/seeding/plan_" + letter + ".json");
        ValidationResult result = GridValidator.validate(plan);
        assertTrue(result.isValid(),
                () -> "План " + letter + " нарушает инварианты:\n - " + String.join("\n - ", result.violations()));
    }

    @Test
    void plansCoverTenToOneHundredThirtyFiveWithoutGaps() {
        ValidationResult result = GridValidator.validateCoverage(loadAll());
        assertTrue(result.isValid(),
                () -> "Покрытие диапазона нарушено:\n - " + String.join("\n - ", result.violations()));
    }
}
