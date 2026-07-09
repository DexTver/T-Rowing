package ru.rowing.seeding;

import org.junit.jupiter.api.Test;
import ru.rowing.seeding.model.Plan;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridValidatorTest {

    @Test
    void referencePlanAIsValid() {
        Plan planA = PlanRepository.fromClasspath("/seeding/plan_A.json");
        ValidationResult result = GridValidator.validate(planA);
        assertTrue(result.isValid(), () -> "План A должен быть валиден, но: " + result.violations());
    }

    @Test
    void missingLaneIsRejected() {
        // вариант "1" без дорожки 9 (нет best_time-назначения)
        String json = """
                {
                  "plan": "X", "participants": {"min":10,"max":18}, "lanes": 9,
                  "prelims": {"count":2,"sizing":"even_desc"},
                  "stages": [{
                    "stage":"semifinal","heats_count":1,
                    "variants": {"1": [
                      {"target":{"heat":1,"lane":1},"source":{"type":"place","place":7,"from":{"stage":"prelim","heat":2}}},
                      {"target":{"heat":1,"lane":2},"source":{"type":"place","place":6,"from":{"stage":"prelim","heat":1}}},
                      {"target":{"heat":1,"lane":3},"source":{"type":"place","place":5,"from":{"stage":"prelim","heat":2}}},
                      {"target":{"heat":1,"lane":4},"source":{"type":"place","place":4,"from":{"stage":"prelim","heat":2}}},
                      {"target":{"heat":1,"lane":5},"source":{"type":"place","place":4,"from":{"stage":"prelim","heat":1}}},
                      {"target":{"heat":1,"lane":6},"source":{"type":"place","place":5,"from":{"stage":"prelim","heat":1}}},
                      {"target":{"heat":1,"lane":7},"source":{"type":"place","place":6,"from":{"stage":"prelim","heat":2}}},
                      {"target":{"heat":1,"lane":8},"source":{"type":"place","place":7,"from":{"stage":"prelim","heat":1}}}
                    ]}
                  }]
                }
                """;
        ValidationResult result = GridValidator.validate(PlanRepository.parse(json));
        assertFalse(result.isValid());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("не заполнены дорожки")),
                () -> result.violations().toString());
    }

    @Test
    void duplicateLaneIsRejected() {
        String json = """
                {
                  "plan": "X", "participants": {"min":10,"max":18}, "lanes": 9,
                  "prelims": {"count":2,"sizing":"even_desc"},
                  "stages": [{
                    "stage":"semifinal","heats_count":1,
                    "variants": {"1": [
                      {"target":{"heat":1,"lane":5},"source":{"type":"place","place":4,"from":{"stage":"prelim","heat":1}}},
                      {"target":{"heat":1,"lane":5},"source":{"type":"place","place":5,"from":{"stage":"prelim","heat":1}}}
                    ]}
                  }]
                }
                """;
        ValidationResult result = GridValidator.validate(PlanRepository.parse(json));
        assertFalse(result.isValid());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("заняты повторно")),
                () -> result.violations().toString());
    }

    @Test
    void duplicateSourceSlotIsRejected() {
        // дорожки покрыты, но место 4/прелим-1 используется дважды (инвариант 2)
        String json = """
                {
                  "plan": "X", "participants": {"min":10,"max":18}, "lanes": 2,
                  "prelims": {"count":2,"sizing":"even_desc"},
                  "stages": [{
                    "stage":"semifinal","heats_count":1,
                    "variants": {"1": [
                      {"target":{"heat":1,"lane":1},"source":{"type":"place","place":4,"from":{"stage":"prelim","heat":1}}},
                      {"target":{"heat":1,"lane":2},"source":{"type":"place","place":4,"from":{"stage":"prelim","heat":1}}}
                    ]}
                  }]
                }
                """;
        ValidationResult result = GridValidator.validate(PlanRepository.parse(json));
        assertFalse(result.isValid());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("используется более одного раза")),
                () -> result.violations().toString());
    }

    @Test
    void prelimHeatExceedingLanesIsRejected() {
        // count=1, N до 18, дорожек 9 -> заезд из 18 не помещается (инвариант 3)
        String json = """
                {
                  "plan": "X", "participants": {"min":10,"max":18}, "lanes": 9,
                  "prelims": {"count":1,"sizing":"even_desc"},
                  "stages": []
                }
                """;
        ValidationResult result = GridValidator.validate(PlanRepository.parse(json));
        assertFalse(result.isValid());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("превышает")),
                () -> result.violations().toString());
    }

    @Test
    void coverageReportsGapsWhenOnlyPlanAPresent() {
        Plan planA = PlanRepository.fromClasspath("/seeding/plan_A.json");
        ValidationResult result = GridValidator.validateCoverage(List.of(planA));
        assertFalse(result.isValid());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("19..162")),
                () -> result.violations().toString());
    }

    @Test
    void coverageOkForContiguousNonOverlappingRanges() {
        // искусственные планы, полностью покрывающие 10..162 без дыр и пересечений
        Plan a = PlanRepository.parse("{\"plan\":\"A\",\"participants\":{\"min\":10,\"max\":70},\"lanes\":9}");
        Plan b = PlanRepository.parse("{\"plan\":\"B\",\"participants\":{\"min\":71,\"max\":162},\"lanes\":9}");
        ValidationResult result = GridValidator.validateCoverage(List.of(a, b));
        assertEquals(List.of(), result.violations());
    }
}
