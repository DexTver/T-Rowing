package ru.rowing.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.rowing.app.domain.BoatClass;
import ru.rowing.app.domain.Category;
import ru.rowing.app.domain.Competition;
import ru.rowing.app.domain.Gender;
import ru.rowing.app.domain.Heat;
import ru.rowing.app.domain.Result;
import ru.rowing.app.domain.Stage;
import ru.rowing.app.domain.StageType;
import ru.rowing.app.repo.CategoryRepository;
import ru.rowing.app.repo.HeatRepository;
import ru.rowing.app.repo.ResultRepository;
import ru.rowing.app.repo.StageRepository;
import ru.rowing.app.service.JudgeService;
import ru.rowing.seeding.runtime.ResultStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Создание с нуля для 10–18 экипажей: по умолчанию A-alt (2 п/ф жеребьёвкой), с возможностью переключить на A. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AAltFlowTest {

    @Autowired JudgeService judge;
    @Autowired CategoryRepository categories;
    @Autowired StageRepository stages;
    @Autowired HeatRepository heats;
    @Autowired ResultRepository results;

    private long newCategoryWith(int participants) {
        Competition comp = judge.createCompetition("Кубок", null);
        Category cat = judge.addCategory(comp.getId(), null, "K1 200", BoatClass.K1, Gender.MALE, 200,
                null, null, false, false, null, null);
        judge.openRegistration(cat.getId());
        for (int i = 1; i <= participants; i++) {
            judge.addParticipant(cat.getId(), "Спортсмен " + i, 2010, "Тест", "СШ");
        }
        return cat.getId();
    }

    @Test
    void tenToEighteenDefaultsToAAltWithTwoSemifinals() {
        long catId = newCategoryWith(12);
        judge.closeRegistrationAndFormProtocol(catId);

        Category cat = categories.findById(catId).orElseThrow();
        assertEquals("A-alt", cat.getPlan(), "по умолчанию для 10–18 — A-alt");

        List<Stage> st = stages.findByCategoryIdOrderByOrdinal(catId);
        assertEquals(1, st.size());
        assertEquals(StageType.SEMIFINAL, st.get(0).getType(), "жеребьёвка сразу в полуфиналы");
        assertEquals(2, heats.findByStageId(st.get(0).getId()).size(), "2 полуфинала");

        // вносим результаты п/ф и формируем финал
        enterAll(st.get(0));
        judge.formNextStage(catId);
        Stage fin = stages.findByCategoryIdOrderByOrdinal(catId).stream()
                .filter(s -> s.getType() == StageType.FINAL).findFirst().orElseThrow();
        long lanes = heats.findByStageId(fin.getId()).stream()
                .mapToLong(h -> results.findByHeatId(h.getId()).size()).sum();
        assertTrue(lanes >= 8, "финал сформирован из полуфиналов: " + lanes);
    }

    @Test
    void canSwitchToStandardPlanA() {
        long catId = newCategoryWith(12);
        judge.setPlan(catId, "A");
        judge.closeRegistrationAndFormProtocol(catId);

        Category cat = categories.findById(catId).orElseThrow();
        assertEquals("A", cat.getPlan());
        List<Stage> st = stages.findByCategoryIdOrderByOrdinal(catId);
        assertEquals(StageType.PRELIM, st.get(0).getType(), "стандартный A: сначала предварительные");
    }

    private void enterAll(Stage stage) {
        long t = 60_000;
        for (Heat h : heats.findByStageId(stage.getId())) {
            for (Result r : results.findByHeatId(h.getId())) {
                r.setStatus(ResultStatus.OK);
                r.setTimeMs(t += 1111);
                results.save(r);
            }
        }
    }
}
