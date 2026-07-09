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
import ru.rowing.seeding.runtime.StageResults;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** План A-alt: жеребьёвка сразу в 2 полуфинала (без предвар), финал — посевом из полуфиналов. */
class PlanAAltTest {

    private final Plan plan = PlanRepository.fromClasspath("/seeding/plan_A-alt.json");
    private final SeedingEngine engine = new SeedingEngine();

    @Test
    void planIsValid() {
        assertTrue(GridValidator.validate(plan).isValid(),
                () -> GridValidator.validate(plan).violations().toString());
    }

    @Test
    void drawsIntoTwoSemifinalsReproducibly() {
        List<Long> ids = new ArrayList<>();
        for (long i = 1; i <= 14; i++) {
            ids.add(i);
        }
        StageDraw a = engine.drawSemifinals(plan, ids, 42L);
        StageDraw b = engine.drawSemifinals(plan, ids, 42L);
        assertEquals(a.heats(), b.heats(), "одно зерно — одинаковая раскладка");
        assertEquals(2, a.heats().size(), "2 полуфинала");
        assertEquals(7, a.heats().get(0).lanes().size());
        assertEquals(7, a.heats().get(1).lanes().size());
        long distinct = a.heats().stream().flatMap(h -> h.lanes().stream())
                .map(LaneAssignment::athleteId).distinct().count();
        assertEquals(14, distinct);
    }

    @Test
    void finalFormedFromSemifinalResults() {
        // 2 полуфинала по 7; id = заезд*10 + место, время = место*1000 + заезд
        List<HeatResults> semis = new ArrayList<>();
        for (int heat = 1; heat <= 2; heat++) {
            List<ResultEntry> rows = new ArrayList<>();
            for (int place = 1; place <= 7; place++) {
                rows.add(new ResultEntry(heat * 10L + place, place, place * 1000L + heat, ResultStatus.OK));
            }
            semis.add(new HeatResults(heat, rows));
        }
        StageHistory history = StageHistory.of(new StageResults("semifinal", semis));

        StageDraw fin = engine.formNextStage(plan, "default", "final", history, Set.of("A"));
        HeatDraw finalA = fin.finalHeat("A").orElseThrow();
        assertEquals(9, finalA.lanes().size(), "финал: 1-4 каждого п/ф + лучший по времени = 9");

        Set<Long> ids = finalA.lanes().stream().map(LaneAssignment::athleteId).collect(Collectors.toSet());
        assertEquals(9, ids.size(), "никого дважды");
        // места 1–4 каждого п/ф проходят напрямую
        assertTrue(ids.containsAll(List.of(11L, 12L, 13L, 14L, 21L, 22L, 23L, 24L)));
    }
}
