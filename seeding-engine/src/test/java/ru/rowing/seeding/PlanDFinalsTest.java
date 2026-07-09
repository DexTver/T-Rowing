package ru.rowing.seeding;

import org.junit.jupiter.api.Test;
import ru.rowing.seeding.draw.HeatDraw;
import ru.rowing.seeding.draw.LaneAssignment;
import ru.rowing.seeding.draw.StageDraw;
import ru.rowing.seeding.model.Plan;
import ru.rowing.seeding.runtime.HeatResults;
import ru.rowing.seeding.runtime.ResultEntry;
import ru.rowing.seeding.runtime.ResultStatus;
import ru.rowing.seeding.runtime.StageHistory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверка последовательного отбора по времени в планах с финалами A/B/C (на примере D):
 * пулы best_time/place_by_time не должны пересекаться (никого не назначаем дважды).
 */
class PlanDFinalsTest {

    private final Plan planD = PlanRepository.fromClasspath("/seeding/plan_D.json");
    private final SeedingEngine engine = new SeedingEngine();

    /** Полуфинал плана D: 4 заезда по 9. id = заезд*10 + место; время = место*1000 + заезд. */
    private StageHistory semifinalsOfD() {
        List<HeatResults> heats = new ArrayList<>();
        for (int heat = 1; heat <= 4; heat++) {
            List<ResultEntry> rows = new ArrayList<>();
            for (int place = 1; place <= 9; place++) {
                long id = heat * 10L + place;
                long time = place * 1000L + heat;
                rows.add(new ResultEntry(id, place, time, ResultStatus.OK));
            }
            heats.add(new HeatResults(heat, rows));
        }
        return StageHistory.of(new ru.rowing.seeding.runtime.StageResults("semifinal", heats));
    }

    @Test
    void allThreeFinalsAreDisjointAndFull() {
        StageDraw fin = engine.formNextStage(planD, "default", "final", semifinalsOfD(), Set.of("A", "B", "C"));

        List<Long> all = new ArrayList<>();
        for (String letter : List.of("A", "B", "C")) {
            HeatDraw heat = fin.finalHeat(letter).orElseThrow(() -> new AssertionError("нет финала " + letter));
            assertEquals(9, heat.lanes().size(), "финал " + letter + " должен иметь 9 дорожек");
            heat.lanes().forEach(la -> all.add(la.athleteId()));
        }

        assertEquals(27, all.size());
        Set<Long> distinct = new HashSet<>(all);
        assertEquals(27, distinct.size(), "ни один спортсмен не должен попасть в два финала: " + all);
    }

    @Test
    void finalAIsIdenticalWhetherBcEnabledOrNot() {
        StageHistory history = semifinalsOfD();
        StageDraw onlyA = engine.formNextStage(planD, "default", "final", history, Set.of("A"));
        StageDraw withBc = engine.formNextStage(planD, "default", "final", history, Set.of("A", "B", "C"));

        // только A: финалов B/C нет
        assertTrue(onlyA.finalHeat("B").isEmpty());
        assertTrue(onlyA.finalHeat("C").isEmpty());

        // раскладка финала A не зависит от того, включены ли B/C (инвариант, не зависит от содержимого плана)
        assertEquals(lanesOf(onlyA.finalHeat("A").orElseThrow()),
                lanesOf(withBc.finalHeat("A").orElseThrow()));
    }

    /** Селектор place_by_time («X из Y-ых по времени») на синтетическом плане — независимо от файлов сеток. */
    @Test
    void placeByTimeSelectorPicksNthOfPlaceRankedByTime() {
        Plan plan = PlanRepository.parse("""
                { "plan": "T", "participants": {"min":10,"max":18}, "lanes": 2,
                  "prelims": {"count":2,"sizing":"even_desc"},
                  "stages": [{ "stage":"final", "finals":["A"], "variants": {"default":[
                    {"target":{"final":"A","lane":1},"source":{"type":"place_by_time","place":3,"rank":1,"pool":{"stage":"semifinal"}}},
                    {"target":{"final":"A","lane":2},"source":{"type":"place_by_time","place":3,"rank":2,"pool":{"stage":"semifinal"}}}
                  ]}}] }
                """);
        // два полуфинала; 3-и места: id 13 (время 300) и id 23 (время 350)
        HeatResults h1 = new HeatResults(1, List.of(
                new ResultEntry(11, 1, 100L, ResultStatus.OK),
                new ResultEntry(12, 2, 200L, ResultStatus.OK),
                new ResultEntry(13, 3, 300L, ResultStatus.OK)));
        HeatResults h2 = new HeatResults(2, List.of(
                new ResultEntry(21, 1, 150L, ResultStatus.OK),
                new ResultEntry(22, 2, 250L, ResultStatus.OK),
                new ResultEntry(23, 3, 350L, ResultStatus.OK)));
        StageHistory history = StageHistory.of(new ru.rowing.seeding.runtime.StageResults("semifinal", List.of(h1, h2)));

        StageDraw fin = engine.formNextStage(plan, "default", "final", history, Set.of("A"));
        HeatDraw finalA = fin.finalHeat("A").orElseThrow();
        // rank 1 (самый быстрый из 3-их) -> дорожка 1 = id 13; rank 2 -> дорожка 2 = id 23
        assertEquals(13L, laneAthlete(finalA, 1));
        assertEquals(23L, laneAthlete(finalA, 2));
    }

    private static long laneAthlete(HeatDraw heat, int lane) {
        return heat.lanes().stream().filter(la -> la.lane() == lane).findFirst().orElseThrow().athleteId();
    }

    private static List<LaneAssignment> lanesOf(HeatDraw heat) {
        return heat.lanes();
    }
}
