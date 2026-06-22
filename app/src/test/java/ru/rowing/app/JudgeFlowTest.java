package ru.rowing.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.rowing.app.domain.BoatClass;
import ru.rowing.app.domain.Category;
import ru.rowing.app.domain.CategoryStatus;
import ru.rowing.app.domain.Competition;
import ru.rowing.app.domain.Gender;
import ru.rowing.app.domain.Heat;
import ru.rowing.app.domain.Result;
import ru.rowing.app.domain.Stage;
import ru.rowing.app.domain.StageType;
import ru.rowing.app.repo.HeatRepository;
import ru.rowing.app.repo.ResultRepository;
import ru.rowing.app.repo.StageRepository;
import ru.rowing.app.service.JudgeService;
import ru.rowing.seeding.runtime.ResultStatus;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JudgeFlowTest {

    @Autowired JudgeService judge;
    @Autowired StageRepository stages;
    @Autowired HeatRepository heats;
    @Autowired ResultRepository results;

    private int timeCounter = 0;

    @Test
    void fullPlanAFlowFromDrawToFinish() {
        Competition comp = judge.createCompetition("Кубок", "описание");
        Category cat = judge.addCategory(comp.getId(), null, "K1 муж 200", BoatClass.K1, Gender.MALE,
                200, 2008, 2009, false, false, null, null);
        judge.openRegistration(cat.getId());
        for (int i = 1; i <= 18; i++) {
            judge.addParticipant(cat.getId(), "Спортсмен " + i, 2008, "Регион", "Школа");
        }

        // Жеребьёвка предварительных (N=18 -> план A, 2 заезда 9+9).
        judge.closeRegistrationAndFormProtocol(cat.getId());
        List<Stage> st = stages.findByCategoryIdOrderByOrdinal(cat.getId());
        assertEquals(1, st.size());
        assertEquals(StageType.PRELIM, st.get(0).getType());
        List<Heat> prelimHeats = heats.findByStageIdOrderByIndexInStage(st.get(0).getId());
        assertEquals(2, prelimHeats.size());
        assertEquals(18, prelimHeats.stream().mapToInt(h -> results.findByHeatId(h.getId()).size()).sum());
        assertEquals(CategoryStatus.PRELIMS_RUNNING, reload(cat).getStatus());
        assertEquals("A", reload(cat).getPlan());
        assertNotNull(reload(cat).getDrawSeed());

        // Предварительные -> полуфинал.
        enterAllResults(cat);
        judge.formNextStage(cat.getId());
        Stage semi = lastStage(cat);
        assertEquals(StageType.SEMIFINAL, semi.getType());
        List<Heat> semiHeats = heats.findByStageIdOrderByIndexInStage(semi.getId());
        assertEquals(1, semiHeats.size());
        assertEquals(9, results.findByHeatId(semiHeats.get(0).getId()).size());
        assertEquals(CategoryStatus.SEMIS_RUNNING, reload(cat).getStatus());

        // Полуфинал -> финал A.
        enterAllResults(cat);
        judge.formNextStage(cat.getId());
        Stage fin = lastStage(cat);
        assertEquals(StageType.FINAL, fin.getType());
        List<Heat> finalHeats = heats.findByStageIdOrderByIndexInStage(fin.getId());
        assertEquals(1, finalHeats.size());
        assertEquals("A", finalHeats.get(0).getFinalLetter());
        assertEquals(9, results.findByHeatId(finalHeats.get(0).getId()).size());
        assertEquals(CategoryStatus.FINALS_RUNNING, reload(cat).getStatus());

        // Внесение результатов финала -> завершено.
        enterAllResults(cat);
        assertEquals(CategoryStatus.FINISHED, reload(cat).getStatus());

        // Сквозная нумерация заездов по соревнованию: 1,2 (предв.), 3 (п/ф), 4 (финал).
        List<Integer> numbers = allHeatNumbers(cat);
        assertEquals(List.of(1, 2, 3, 4), numbers);
    }

    @Test
    void smallFieldGoesStraightToFinal() {
        Competition comp = judge.createCompetition("Малый", null);
        Category cat = judge.addCategory(comp.getId(), null, "C1 муж 200", BoatClass.C1, Gender.MALE,
                200, null, null, false, false, null, null);
        judge.openRegistration(cat.getId());
        for (int i = 1; i <= 6; i++) {
            judge.addParticipant(cat.getId(), "Гребец " + i, 2007, "Регион", "Школа");
        }
        judge.closeRegistrationAndFormProtocol(cat.getId());

        List<Stage> st = stages.findByCategoryIdOrderByOrdinal(cat.getId());
        assertEquals(1, st.size());
        assertEquals(StageType.FINAL, st.get(0).getType());
        Heat fin = heats.findByStageIdOrderByIndexInStage(st.get(0).getId()).get(0);
        assertEquals(6, results.findByHeatId(fin.getId()).size());
        assertFalse(fin.isMassStart());
        assertEquals(CategoryStatus.FINALS_RUNNING, reload(cat).getStatus());
    }

    @Test
    void massStartFormsSingleFinalHeat() {
        Competition comp = judge.createCompetition("Марафон", null);
        Category cat = judge.addCategory(comp.getId(), null, "K1 муж 5000", BoatClass.K1, Gender.MALE,
                5000, null, null, false, false, null, null);
        judge.openRegistration(cat.getId());
        for (int i = 1; i <= 12; i++) {
            judge.addParticipant(cat.getId(), "Марафонец " + i, 2006, "Регион", "Школа");
        }
        judge.closeRegistrationAndFormProtocol(cat.getId());

        List<Stage> st = stages.findByCategoryIdOrderByOrdinal(cat.getId());
        assertEquals(1, st.size());
        assertEquals(StageType.FINAL, st.get(0).getType());
        Heat fin = heats.findByStageIdOrderByIndexInStage(st.get(0).getId()).get(0);
        assertTrue(fin.isMassStart());
        assertEquals(12, results.findByHeatId(fin.getId()).size());
        assertEquals(CategoryStatus.FINALS_RUNNING, reload(cat).getStatus());
    }

    // --- helpers ---

    @Autowired ru.rowing.app.repo.CategoryRepository categoriesRepo;

    private Category reload(Category cat) {
        return categoriesRepo.findById(cat.getId()).orElseThrow();
    }

    private Stage lastStage(Category cat) {
        List<Stage> st = stages.findByCategoryIdOrderByOrdinal(cat.getId());
        return st.get(st.size() - 1);
    }

    private void enterAllResults(Category cat) {
        Stage latest = lastStage(cat);
        for (Heat h : heats.findByStageIdOrderByIndexInStage(latest.getId())) {
            StringBuilder input = new StringBuilder();
            for (Result r : results.findByHeatId(h.getId())) {
                timeCounter++;
                input.append(r.getLane()).append(' ')
                        .append(String.format("%d.%02d", 40 + timeCounter / 100, timeCounter % 100))
                        .append('\n');
            }
            List<String> errors = judge.saveResults(h.getId(), input.toString());
            assertTrue(errors.isEmpty(), () -> "ошибки ввода: " + errors);
        }
        // все результаты этапа внесены
        for (Heat h : heats.findByStageId(latest.getId())) {
            assertTrue(results.findByHeatId(h.getId()).stream()
                    .noneMatch(r -> r.getStatus() == ResultStatus.NOT_STARTED));
        }
    }

    private List<Integer> allHeatNumbers(Category cat) {
        List<Integer> numbers = new ArrayList<>();
        for (Stage s : stages.findByCategoryIdOrderByOrdinal(cat.getId())) {
            for (Heat h : heats.findByStageIdOrderByIndexInStage(s.getId())) {
                numbers.add(h.getNumber());
            }
        }
        numbers.sort(Integer::compareTo);
        return numbers;
    }
}
