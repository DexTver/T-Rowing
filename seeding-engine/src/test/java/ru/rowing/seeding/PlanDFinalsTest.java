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

        // раскладка финала A не зависит от того, включены ли B/C
        assertEquals(lanesOf(onlyA.finalHeat("A").orElseThrow()),
                lanesOf(withBc.finalHeat("A").orElseThrow()));

        // лучший по времени в финал A (дорожка 1) = самый быстрый из 3-их мест = заезд 1, место 3 -> id 13
        assertEquals(13L, onlyA.finalHeat("A").orElseThrow().lanes().stream()
                .filter(la -> la.lane() == 1).findFirst().orElseThrow().athleteId());
    }

    @Test
    void secondByTimeFromThirdPlacesResolvesCorrectly() {
        // place_by_time(place=3, rank=2) в финале B = 2-й по времени среди 3-их мест = id 23 (заезд 2, место 3)
        StageDraw fin = engine.formNextStage(planD, "default", "final", semifinalsOfD(), Set.of("A", "B", "C"));
        Set<Long> b = new HashSet<>();
        fin.finalHeat("B").orElseThrow().lanes().forEach(la -> b.add(la.athleteId()));
        assertTrue(b.contains(23L), "2-й по времени из 3-их мест (id 23) должен быть в финале B: " + b);
        // и он не должен одновременно оказаться в финале A
        Set<Long> a = new HashSet<>();
        fin.finalHeat("A").orElseThrow().lanes().forEach(la -> a.add(la.athleteId()));
        assertFalse(a.contains(23L));
    }

    private static List<LaneAssignment> lanesOf(HeatDraw heat) {
        return heat.lanes();
    }
}
