package ru.rowing.seeding;

import org.junit.jupiter.api.Test;
import ru.rowing.seeding.draw.HeatDraw;
import ru.rowing.seeding.draw.LaneAssignment;
import ru.rowing.seeding.draw.PrelimDraw;
import ru.rowing.seeding.draw.StageDraw;
import ru.rowing.seeding.model.Plan;
import ru.rowing.seeding.runtime.HeatResults;
import ru.rowing.seeding.runtime.ResultEntry;
import ru.rowing.seeding.runtime.ResultStatus;
import ru.rowing.seeding.runtime.StageHistory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Эталонная раскладка Плана A (раздел 8.5 ТЗ). */
class PlanAReferenceTest {

    private final Plan planA = PlanRepository.fromClasspath("/seeding/plan_A.json");
    private final SeedingEngine engine = new SeedingEngine();
    private static final Set<String> ONLY_A = Set.of("A");

    /** Карта дорожка → id спортсмена для заезда. */
    private static Map<Integer, Long> lanes(HeatDraw heat) {
        Map<Integer, Long> map = new HashMap<>();
        for (LaneAssignment la : heat.lanes()) {
            map.put(la.lane(), la.athleteId());
        }
        return map;
    }

    @Test
    void preliminaryDrawIsReproducibleBySeed() {
        List<Long> ids = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);
        PrelimDraw a = engine.drawPreliminaries(planA, ids, 42L);
        PrelimDraw b = engine.drawPreliminaries(planA, ids, 42L);
        assertEquals(a.heats(), b.heats(), "одно зерно — одинаковая раскладка");

        // N=12 -> 2 заезда по 6 (even_desc), всего 12 уникальных спортсменов
        assertEquals(2, a.heats().size());
        assertEquals(6, a.heats().get(0).lanes().size());
        assertEquals(6, a.heats().get(1).lanes().size());
        long distinct = a.heats().stream().flatMap(h -> h.lanes().stream())
                .map(LaneAssignment::athleteId).distinct().count();
        assertEquals(12, distinct);
    }

    @Test
    void semifinalSeedingMatchesVariant1() {
        StageHistory history = StageHistory.of(
                TestResults.stage("prelim", TestResults.prelimHeat(1, 9), TestResults.prelimHeat(2, 9)));

        StageDraw semi = engine.formNextStage(planA, "1", "semifinal", history, ONLY_A);
        Map<Integer, Long> lane = lanes(semi.semifinal(1).orElseThrow());

        // ожидание из раздела 8.5, вариант "1"; id = heat*100 + place
        assertEquals(207L, lane.get(1));
        assertEquals(106L, lane.get(2));
        assertEquals(205L, lane.get(3));
        assertEquals(204L, lane.get(4));
        assertEquals(104L, lane.get(5));
        assertEquals(105L, lane.get(6));
        assertEquals(206L, lane.get(7));
        assertEquals(107L, lane.get(8));
        assertEquals(108L, lane.get(9)); // лучший по времени среди мест 8–9
        assertEquals(9, lane.size());
    }

    @Test
    void semifinalSeedingMatchesVariant2() {
        StageHistory history = StageHistory.of(
                TestResults.stage("prelim", TestResults.prelimHeat(1, 9), TestResults.prelimHeat(2, 9)));

        StageDraw semi = engine.formNextStage(planA, "2", "semifinal", history, ONLY_A);
        Map<Integer, Long> lane = lanes(semi.semifinal(1).orElseThrow());

        assertEquals(107L, lane.get(1));
        assertEquals(206L, lane.get(2));
        assertEquals(105L, lane.get(3));
        assertEquals(104L, lane.get(4));
        assertEquals(204L, lane.get(5));
        assertEquals(205L, lane.get(6));
        assertEquals(106L, lane.get(7));
        assertEquals(207L, lane.get(8));
        assertEquals(108L, lane.get(9));
        assertEquals(9, lane.size());
    }

    @Test
    void finalSeedingMatchesDefault() {
        // полуфинал: 9 участников, id = время (меньше — выше место) -> place1=104, place2=105, place3=106
        long[] semiIds = {104, 105, 106, 107, 204, 205, 206, 207, 108};
        ResultEntry[] semiRows = new ResultEntry[semiIds.length];
        for (int i = 0; i < semiIds.length; i++) {
            semiRows[i] = new ResultEntry(semiIds[i], i + 1, semiIds[i], ResultStatus.OK);
        }
        StageHistory history = StageHistory.of(
                TestResults.stage("prelim", TestResults.prelimHeat(1, 9), TestResults.prelimHeat(2, 9)),
                TestResults.stage("semifinal", new HeatResults(1, List.of(semiRows))));

        StageDraw fin = engine.formNextStage(planA, "default", "final", history, ONLY_A);
        Map<Integer, Long> lane = lanes(fin.finalHeat("A").orElseThrow());

        assertEquals(101L, lane.get(5));
        assertEquals(102L, lane.get(3));
        assertEquals(103L, lane.get(7));
        assertEquals(201L, lane.get(4));
        assertEquals(202L, lane.get(6));
        assertEquals(203L, lane.get(2));
        assertEquals(104L, lane.get(8)); // 1-е место полуфинала
        assertEquals(105L, lane.get(1)); // 2-е место полуфинала
        assertEquals(106L, lane.get(9)); // 3-е место полуфинала
        assertEquals(9, lane.size());
    }

    @Test
    void tieAtTimeBoundaryAddsTenthLane() {
        // Места 1–7 одинаковы в обоих заездах; пул «по времени» = места 8,9.
        // id108 и id208 делят лучшее время пула (8000) -> оба проходят, второй на 10-ю дорожку.
        HeatResults heat1 = new HeatResults(1, List.of(
                place(101, 1, 1000), place(102, 2, 2000), place(103, 3, 3000),
                place(104, 4, 4000), place(105, 5, 5000), place(106, 6, 6000),
                place(107, 7, 7000), place(108, 8, 8000), place(109, 9, 9000)));
        HeatResults heat2 = new HeatResults(2, List.of(
                place(201, 1, 1000), place(202, 2, 2000), place(203, 3, 3000),
                place(204, 4, 4000), place(205, 5, 5000), place(206, 6, 6000),
                place(207, 7, 7000), place(208, 8, 8000), place(209, 9, 9500)));
        StageHistory history = StageHistory.of(TestResults.stage("prelim", heat1, heat2));

        StageDraw semi = engine.formNextStage(planA, "1", "semifinal", history, ONLY_A);
        Map<Integer, Long> lane = lanes(semi.semifinal(1).orElseThrow());

        assertEquals(10, lane.size(), "при равенстве на границе добавляется 10-я дорожка");
        assertEquals(108L, lane.get(9));
        assertEquals(208L, lane.get(10));
    }

    @Test
    void smallFieldGoesStraightToFinal() {
        List<Long> ids = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L); // N<10
        StageDraw fin = engine.directFinal(ids, 7L);
        assertEquals("final", fin.stage());
        HeatDraw a = fin.finalHeat("A").orElseThrow();
        assertEquals(7, a.lanes().size());
        long distinct = a.lanes().stream().map(LaneAssignment::athleteId).distinct().count();
        assertEquals(7, distinct);
        assertTrue(a.lanes().stream().allMatch(la -> la.lane() >= 1 && la.lane() <= 7));
    }

    private static ResultEntry place(long id, int lane, long timeMs) {
        return new ResultEntry(id, lane, timeMs, ResultStatus.OK);
    }
}
