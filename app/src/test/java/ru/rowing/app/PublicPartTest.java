package ru.rowing.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.rowing.app.domain.BoatClass;
import ru.rowing.app.domain.Category;
import ru.rowing.app.domain.CategoryStatus;
import ru.rowing.app.domain.Competition;
import ru.rowing.app.domain.CompetitionStatus;
import ru.rowing.app.domain.Gender;
import ru.rowing.app.domain.Heat;
import ru.rowing.app.domain.Result;
import ru.rowing.app.domain.Stage;
import ru.rowing.app.domain.StageType;
import ru.rowing.app.repo.CategoryRepository;
import ru.rowing.app.repo.CompetitionRepository;
import ru.rowing.app.repo.HeatRepository;
import ru.rowing.app.repo.ResultRepository;
import ru.rowing.app.repo.StageRepository;
import ru.rowing.app.service.PublicViewService;
import ru.rowing.app.web.view.PublicViews.HeatView;
import ru.rowing.seeding.runtime.ResultStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PublicPartTest {

    @Autowired MockMvc mvc;
    @Autowired PublicViewService view;
    @Autowired CompetitionRepository competitions;
    @Autowired CategoryRepository categories;
    @Autowired StageRepository stages;
    @Autowired HeatRepository heats;
    @Autowired ResultRepository results;

    private long competitionId;
    private long runHeatId;
    private long notRunHeatId;
    private long hiddenHeatId;

    @BeforeEach
    void seed() {
        Competition comp = new Competition();
        comp.setName("Тестовый кубок");
        comp.setStatus(CompetitionStatus.LIVE);
        comp = competitions.save(comp);
        competitionId = comp.getId();

        // Видимая категория (протокол сформирован).
        Category cat = newCategory(comp, "K1 мужчины 200 м", CategoryStatus.PRELIMS_RUNNING);
        Stage prelim = newStage(cat, StageType.PRELIM, 1);

        Heat run = newHeat(prelim, 1, 1);
        runHeatId = run.getId();
        // дорожки 1,2,3 с временами вне порядка дорожек -> проверяем сортировку по времени
        result(run, 1, ResultStatus.OK, 42_000L);
        result(run, 2, ResultStatus.OK, 41_000L);
        result(run, 3, ResultStatus.DNF, null);

        Heat notRun = newHeat(prelim, 2, 2);
        notRunHeatId = notRun.getId();
        result(notRun, 2, ResultStatus.NOT_STARTED, null);
        result(notRun, 1, ResultStatus.NOT_STARTED, null);

        // Скрытая категория (регистрация открыта) — её протокол показывать нельзя.
        Category hidden = newCategory(comp, "C1 женщины 500 м", CategoryStatus.REGISTRATION_OPEN);
        Stage hp = newStage(hidden, StageType.PRELIM, 1);
        hiddenHeatId = newHeat(hp, 99, 1).getId();
    }

    @Test
    void homePageLists() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Тестовый кубок")));
    }

    @Test
    void resultsPageWithoutSearchRenders() throws Exception {
        mvc.perform(get("/results"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Результаты и фильтры")));
    }

    @Test
    void allPublicTemplatesRender() throws Exception {
        mvc.perform(get("/competitions/{id}", competitionId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Заезды")));
        mvc.perform(get("/heats/{id}", runHeatId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Спортсмен Тестовый")));
        mvc.perform(get("/results").param("region", "Москва"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Спортсмен Тестовый")));
    }

    private static java.util.List<ru.rowing.app.web.view.PublicViews.HeatCard> allHeats(
            ru.rowing.app.web.view.PublicViews.CompetitionView v) {
        return v.days.stream().flatMap(d -> d.heats.stream()).toList();
    }

    @Test
    void competitionShowsOnlyVisibleHeats() {
        var comp = view.getCompetition(competitionId).orElseThrow();
        var heatsList = allHeats(comp);
        // 2 видимых заезда; скрытый (№99) не входит
        assertEquals(2, heatsList.size());
        assertTrue(heatsList.stream().noneMatch(h -> h.number == 99));
    }

    @Test
    void heatsSortedByNumberWithinDay() {
        Competition comp = new Competition();
        comp.setName("Порядок по номеру");
        comp.setStatus(CompetitionStatus.LIVE);
        comp = competitions.save(comp);
        Category cat = newCategory(comp, "K1 500", CategoryStatus.FINALS_RUNNING);
        // создаём вперемешку; ожидаем сортировку по номеру заезда (= по времени)
        newHeat(newStage(cat, StageType.FINAL, 3), 3, 1);
        newHeat(newStage(cat, StageType.SEMIFINAL, 2), 2, 1);
        newHeat(newStage(cat, StageType.PRELIM, 1), 1, 1);

        var heatsList = allHeats(view.getCompetition(comp.getId()).orElseThrow());
        assertEquals(3, heatsList.size());
        assertEquals(1, heatsList.get(0).number);
        assertEquals(2, heatsList.get(1).number);
        assertEquals(3, heatsList.get(2).number);
    }

    @Test
    void hiddenHeatIsNotAccessible() {
        assertTrue(view.getHeat(hiddenHeatId).isEmpty());
    }

    @Test
    void runHeatRankedByTimeWithDnfLast() {
        HeatView h = view.getHeat(runHeatId).orElseThrow();
        assertTrue(h.run);
        // порядок: дорожка 2 (41.0, место 1), дорожка 1 (42.0, место 2), затем DNF (дорожка 3)
        assertEquals(2, h.results.get(0).lane);
        assertEquals(1, h.results.get(0).place);
        assertEquals(1, h.results.get(1).lane);
        assertEquals(2, h.results.get(1).place);
        assertEquals(3, h.results.get(2).lane);
        assertNull(h.results.get(2).place);
        assertEquals("DNF", h.results.get(2).statusLabel);
    }

    @Test
    void notRunHeatOrderedByLane() {
        HeatView h = view.getHeat(notRunHeatId).orElseThrow();
        assertFalse(h.run);
        assertEquals(1, h.results.get(0).lane);
        assertEquals(2, h.results.get(1).lane);
    }

    @Test
    void searchFiltersByRegion() {
        var rows = view.search(null, null, null, "Тестовый-несуществующий-регион", null);
        assertTrue(rows.isEmpty());
    }

    // --- helpers ---

    private Category newCategory(Competition comp, String name, CategoryStatus status) {
        Category c = new Category();
        c.setCompetition(comp);
        c.setName(name);
        c.setBoatClass(BoatClass.K1);
        c.setGender(Gender.MALE);
        c.setDistanceM(200);
        c.setStatus(status);
        return categories.save(c);
    }

    private Stage newStage(Category cat, StageType type, int ordinal) {
        Stage s = new Stage();
        s.setCategory(cat);
        s.setType(type);
        s.setOrdinal(ordinal);
        return stages.save(s);
    }

    private Heat newHeat(Stage stage, int number, int indexInStage) {
        Heat h = new Heat();
        h.setStage(stage);
        h.setNumber(number);
        h.setIndexInStage(indexInStage);
        return heats.save(h);
    }

    private void result(Heat heat, int lane, ResultStatus status, Long timeMs) {
        Result r = new Result();
        r.setHeat(heat);
        r.setAthlete(athlete());
        r.setLane(lane);
        r.setStatus(status);
        r.setTimeMs(timeMs);
        results.save(r);
    }

    @Autowired ru.rowing.app.repo.AthleteRepository athletes;

    private ru.rowing.app.domain.Athlete athlete() {
        ru.rowing.app.domain.Athlete a = new ru.rowing.app.domain.Athlete();
        a.setFullName("Спортсмен Тестовый");
        a.setBirthYear(2008);
        a.setRegion("Москва");
        a.setSportSchool("СШОР Тест");
        return athletes.save(a);
    }
}
