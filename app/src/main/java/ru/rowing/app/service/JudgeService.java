package ru.rowing.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rowing.app.domain.Athlete;
import ru.rowing.app.domain.BoatClass;
import ru.rowing.app.domain.Category;
import ru.rowing.app.domain.CategoryStatus;
import ru.rowing.app.domain.Competition;
import ru.rowing.app.domain.CompetitionDay;
import ru.rowing.app.domain.Entry;
import ru.rowing.app.domain.Gender;
import ru.rowing.app.domain.Heat;
import ru.rowing.app.domain.Result;
import ru.rowing.app.domain.Stage;
import ru.rowing.app.domain.StageType;
import ru.rowing.app.repo.AthleteRepository;
import ru.rowing.app.repo.CategoryRepository;
import ru.rowing.app.repo.CompetitionDayRepository;
import ru.rowing.app.repo.CompetitionRepository;
import ru.rowing.app.repo.EntryRepository;
import ru.rowing.app.repo.HeatRepository;
import ru.rowing.app.repo.ResultRepository;
import ru.rowing.app.repo.StageRepository;
import ru.rowing.seeding.SeedingException;
import ru.rowing.seeding.draw.HeatDraw;
import ru.rowing.seeding.draw.LaneAssignment;
import ru.rowing.seeding.draw.PrelimDraw;
import ru.rowing.seeding.draw.StageDraw;
import ru.rowing.seeding.model.Plan;
import ru.rowing.seeding.runtime.HeatResults;
import ru.rowing.seeding.runtime.ResultEntry;
import ru.rowing.seeding.runtime.ResultStatus;
import ru.rowing.seeding.runtime.StageHistory;
import ru.rowing.seeding.runtime.StageResults;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Оркестрация хода соревнования судьёй (раздел 7.3 ТЗ): создание соревнования и категорий,
 * закрытие регистрации с жеребьёвкой, ввод результатов и формирование следующих этапов через
 * {@code seeding-engine}. Формирование детерминировано (зерно жеребьёвки сохраняется).
 */
@Service
@Transactional
public class JudgeService {

    private final CompetitionRepository competitions;
    private final CompetitionDayRepository days;
    private final CategoryRepository categories;
    private final AthleteRepository athletes;
    private final EntryRepository entries;
    private final StageRepository stages;
    private final HeatRepository heats;
    private final ResultRepository results;
    private final PlanCatalog catalog;

    public JudgeService(CompetitionRepository competitions, CompetitionDayRepository days,
                        CategoryRepository categories, AthleteRepository athletes, EntryRepository entries,
                        StageRepository stages, HeatRepository heats, ResultRepository results,
                        PlanCatalog catalog) {
        this.competitions = competitions;
        this.days = days;
        this.categories = categories;
        this.athletes = athletes;
        this.entries = entries;
        this.stages = stages;
        this.heats = heats;
        this.results = results;
        this.catalog = catalog;
    }

    // --- создание соревнования (раздел 7.3 «Создать соревнование») ----------------------

    public Competition createCompetition(String name, String description) {
        Competition c = new Competition();
        c.setName(name);
        c.setDescription(description);
        c.setStatus(ru.rowing.app.domain.CompetitionStatus.SCHEDULED);
        return competitions.save(c);
    }

    public CompetitionDay addDay(long competitionId, LocalDate date, LocalTime startTime) {
        Competition comp = competition(competitionId);
        CompetitionDay day = new CompetitionDay();
        day.setCompetition(comp);
        day.setOrdinal(days.findByCompetitionIdOrderByOrdinal(competitionId).size() + 1);
        day.setDayDate(date);
        day.setStartTime(startTime);
        return days.save(day);
    }

    public Category addCategory(long competitionId, Long dayId, String name, BoatClass boatClass, Gender gender,
                                int distanceM, Integer birthFrom, Integer birthTo,
                                boolean finalB, boolean finalC,
                                Instant regOpens, Instant regCloses) {
        Category cat = new Category();
        cat.setCompetition(competition(competitionId));
        if (dayId != null) {
            cat.setCompetitionDay(days.findById(dayId).orElseThrow(() -> notFound("День", dayId)));
        }
        cat.setName(name);
        cat.setBoatClass(boatClass);
        cat.setGender(gender);
        cat.setDistanceM(distanceM);
        cat.setBirthYearFrom(birthFrom);
        cat.setBirthYearTo(birthTo);
        cat.setFinalBEnabled(finalB);
        cat.setFinalCEnabled(finalC);
        cat.setRegistrationOpensAt(regOpens);
        cat.setRegistrationClosesAt(regCloses);
        cat.setStatus(CategoryStatus.DRAFT);
        return categories.save(cat);
    }

    // --- регистрация ---------------------------------------------------------------------

    public void openRegistration(long categoryId) {
        Category cat = category(categoryId);
        requireStatus(cat, CategoryStatus.DRAFT, CategoryStatus.REGISTRATION_OPEN);
        cat.setStatus(CategoryStatus.REGISTRATION_OPEN);
    }

    /** Судейское «Добавить участника» (раздел 7.3, п.1): новый спортсмен сразу в заявку категории. */
    public void addParticipant(long categoryId, String fullName, int birthYear, String region, String school) {
        Category cat = category(categoryId);
        requireStatus(cat, CategoryStatus.DRAFT, CategoryStatus.REGISTRATION_OPEN);

        Athlete a = new Athlete();
        a.setFullName(fullName);
        a.setBirthYear(birthYear);
        a.setRegion(region);
        a.setSportSchool(school);
        a = athletes.save(a);

        Entry e = new Entry();
        e.setCategory(cat);
        e.setAthlete(a);
        entries.save(e);
    }

    // --- закрытие регистрации и формирование протокола (жеребьёвка) ----------------------

    public void closeRegistrationAndFormProtocol(long categoryId) {
        Category cat = category(categoryId);
        requireStatus(cat, CategoryStatus.REGISTRATION_OPEN);

        List<Entry> list = entries.findByCategoryId(categoryId);
        if (list.isEmpty()) {
            throw new SeedingException("В категории нет заявок — нечего жеребить.");
        }
        Map<Long, Athlete> byId = new LinkedHashMap<>();
        List<Long> ids = new ArrayList<>();
        for (Entry e : list) {
            byId.put(e.getAthlete().getId(), e.getAthlete());
            ids.add(e.getAthlete().getId());
        }
        long seed = new Random().nextLong();
        cat.setDrawSeed(seed);

        if (cat.isMassStart()) {
            formMassStart(cat, ids, byId, seed);
        } else if (ids.size() < PlanCatalog.MIN_PLAN) {
            formSmallFieldFinal(cat, ids, byId, seed);
        } else {
            // План: выбранный судьёй, иначе по умолчанию (для 10–18 — A-alt).
            String letter = cat.getPlan() != null ? cat.getPlan() : catalog.defaultPlanForCount(ids.size());
            if (PlanCatalog.ALT.equals(letter)) {
                formAltSemifinals(cat, ids, byId, seed);
            } else {
                formPrelims(cat, ids, byId, seed);
            }
        }
    }

    /** План A-alt: жеребьёвка сразу в 2 полуфинала (без предварительных); финал — посевом из п/ф. */
    private void formAltSemifinals(Category cat, List<Long> ids, Map<Long, Athlete> byId, long seed) {
        Plan plan = catalog.planByLetter(PlanCatalog.ALT);
        cat.setPlan(PlanCatalog.ALT);
        cat.setActiveVariant(null);

        Stage semi = newStage(cat, StageType.SEMIFINAL);
        StageDraw draw = catalog.engine().drawSemifinals(plan, ids, seed);
        int[] number = {nextHeatNumber(cat)};
        Instant base = stageBaseStart(cat);
        int order = 0;
        for (HeatDraw hd : draw.heats()) {
            createHeat(semi, hd, byId, number, scheduledAt(base, cat, order++), false);
        }
        cat.setStatus(CategoryStatus.SEMIS_RUNNING);
    }

    private void formPrelims(Category cat, List<Long> ids, Map<Long, Athlete> byId, long seed) {
        Plan plan = catalog.planForCount(ids.size());
        cat.setPlan(plan.plan());
        cat.setActiveVariant("1");

        Stage prelim = newStage(cat, StageType.PRELIM);
        PrelimDraw draw = catalog.engine().drawPreliminaries(plan, ids, seed);
        int[] number = {nextHeatNumber(cat)};
        Instant base = stageBaseStart(cat);
        int order = 0;
        for (HeatDraw hd : draw.heats()) {
            createHeat(prelim, hd, byId, number, scheduledAt(base, cat, order++), false);
        }
        cat.setStatus(CategoryStatus.PRELIMS_RUNNING);
    }

    private void formSmallFieldFinal(Category cat, List<Long> ids, Map<Long, Athlete> byId, long seed) {
        // N < 10: система отбора не применяется — сразу финал A одним заездом (раздел 8.2).
        StageDraw draw = catalog.engine().directFinal(ids, seed);
        Stage finalStage = newStage(cat, StageType.FINAL);
        int[] number = {nextHeatNumber(cat)};
        createHeat(finalStage, draw.heats().getFirst(), byId, number, stageBaseStart(cat), false);
        cat.setStatus(CategoryStatus.FINALS_RUNNING);
    }

    private void formMassStart(Category cat, List<Long> ids, Map<Long, Athlete> byId, long seed) {
        // 5000 м: массовый старт, один заезд финала со всеми (раздел 8.10).
        StageDraw draw = catalog.engine().directFinal(ids, seed);
        Stage finalStage = newStage(cat, StageType.FINAL);
        int[] number = {nextHeatNumber(cat)};
        createHeat(finalStage, draw.heats().getFirst(), byId, number, stageBaseStart(cat), true);
        cat.setStatus(CategoryStatus.FINALS_RUNNING);
    }

    // --- формирование следующего этапа по подтверждению судьи ----------------------------

    public void formNextStage(long categoryId) {
        Category cat = category(categoryId);
        List<Stage> existing = stages.findByCategoryIdOrderByOrdinal(categoryId);
        if (existing.isEmpty()) {
            throw new SeedingException("Протокол ещё не сформирован.");
        }
        Stage latest = existing.getLast();
        if (countNotStarted(latest) > 0) {
            throw new SeedingException("Не все результаты текущего этапа внесены.");
        }
        if (latest.getType() == StageType.FINAL) {
            cat.setStatus(CategoryStatus.FINISHED);
            return;
        }

        if (cat.getPlan() == null) {
            throw new SeedingException("Для категории не определён план сетки — доформирование по сетке недоступно "
                    + "(импортирована нестандартная сетка). Заполните заезды вручную.");
        }
        if (cat.getActiveVariant() == null && !catalog.variantOptions(cat.getPlan()).isEmpty()) {
            throw new SeedingException("Сначала выберите вариант сетки.");
        }

        String toStage = latest.getType() == StageType.PRELIM ? "semifinal" : "final";
        StageType newType = latest.getType() == StageType.PRELIM ? StageType.SEMIFINAL : StageType.FINAL;
        Plan plan = catalog.planByLetter(cat.getPlan());
        StageHistory history = buildHistory(existing);

        StageDraw draw = catalog.engine()
                .formNextStage(plan, cat.getActiveVariant(), toStage, history, cat.enabledFinals());

        Map<Long, Athlete> byId = athleteIndex(history);
        Stage created = newStage(cat, newType);
        int[] number = {nextHeatNumber(cat)};
        Instant base = stageBaseStart(cat);
        int order = 0;
        for (HeatDraw hd : draw.heats()) {
            createHeat(created, hd, byId, number, scheduledAt(base, cat, order++), false);
        }
        cat.setStatus(newType == StageType.SEMIFINAL
                ? CategoryStatus.SEMIS_RUNNING : CategoryStatus.FINALS_RUNNING);
    }

    /** Выбор плана сетки до формирования протокола (для 10–18 — {@code A-alt} или стандартный {@code A}). */
    public void setPlan(long categoryId, String letter) {
        Category cat = category(categoryId);
        requireStatus(cat, CategoryStatus.DRAFT, CategoryStatus.REGISTRATION_OPEN);
        if (!PlanCatalog.ALT.equals(letter)) {
            catalog.planByLetter(letter); // бросит, если план неизвестен
        }
        cat.setPlan(letter);
        cat.setActiveVariant(null);
    }

    /** Выбор варианта посева (раздел 8.8): применяется только к ещё не сформированным этапам. */
    public void setVariant(long categoryId, String variant) {
        Category cat = category(categoryId);
        if (cat.getPlan() == null) {
            throw new SeedingException("У категории не задан план сетки.");
        }
        if (!catalog.variantOptions(cat.getPlan()).contains(variant)) {
            throw new SeedingException("Неизвестный вариант «" + variant + "» для плана " + cat.getPlan() + ".");
        }
        cat.setActiveVariant(variant);
    }

    // --- ввод результатов ----------------------------------------------------------------

    /** Применяет быстрый ввод результатов к заезду; возвращает ошибки парсинга (пустой список — успех). */
    public List<String> saveResults(long heatId, String quickInput) {
        Heat heat = heats.findById(heatId).orElseThrow(() -> notFound("Заезд", heatId));
        ResultParser.Parsed parsed = ResultParser.parse(quickInput);

        Map<Integer, Result> byLane = new LinkedHashMap<>();
        for (Result r : results.findByHeatId(heatId)) {
            byLane.put(r.getLane(), r);
        }
        List<String> errors = new ArrayList<>(parsed.errors());
        for (ResultParser.Row row : parsed.rows()) {
            Result target = byLane.get(row.lane());
            if (target == null) {
                errors.add("В заезде нет дорожки " + row.lane() + ".");
                continue;
            }
            target.setStatus(row.status());
            target.setTimeMs(row.timeMs());
        }

        // Если внесены все результаты завершающего (финального) этапа — категория завершена.
        Stage stage = heat.getStage();
        if (stage.getType() == StageType.FINAL && countNotStarted(stage) == 0) {
            stage.getCategory().setStatus(CategoryStatus.FINISHED);
        }
        return errors;
    }

    // --- вспомогательное -----------------------------------------------------------------

    private Stage newStage(Category cat, StageType type) {
        Stage s = new Stage();
        s.setCategory(cat);
        s.setType(type);
        s.setOrdinal(stages.findByCategoryIdOrderByOrdinal(cat.getId()).size() + 1);
        return stages.save(s);
    }

    /** Создаёт заезд по раскладке движка: сквозной номер, дорожки, результаты NOT_STARTED. */
    private void createHeat(Stage stage, HeatDraw hd, Map<Long, Athlete> byId,
                            int[] numberHolder, Instant scheduledStart, boolean massStart) {
        Heat heat = new Heat();
        heat.setStage(stage);
        heat.setCompetitionDay(stage.getCategory().getCompetitionDay());
        heat.setNumber(numberHolder[0]++);
        heat.setIndexInStage(hd.indexInStage() != null ? hd.indexInStage() : indexForFinal(hd.finalLetter()));
        heat.setFinalLetter(hd.finalLetter());
        heat.setMassStart(massStart);
        heat.setScheduledStart(scheduledStart);
        heat = heats.save(heat);

        for (LaneAssignment la : hd.lanes()) {
            Athlete athlete = byId.get(la.athleteId());
            if (athlete == null) {
                athlete = athletes.findById(la.athleteId()).orElseThrow(() -> notFound("Спортсмен", la.athleteId()));
            }
            Result r = new Result();
            r.setHeat(heat);
            r.setAthlete(athlete);
            r.setLane(la.lane());
            r.setStatus(ResultStatus.NOT_STARTED);
            results.save(r);
        }
    }

    private static int indexForFinal(String letter) {
        return switch (letter) {
            case "A" -> 1;
            case "B" -> 2;
            case "C" -> 3;
            default -> 1;
        };
    }

    /** История завершённых этапов для движка (имена этапов как в файлах сетки). */
    private StageHistory buildHistory(List<Stage> existingStages) {
        StageHistory history = new StageHistory();
        for (Stage st : existingStages) {
            String engineName = switch (st.getType()) {
                case PRELIM -> "prelim";
                case SEMIFINAL -> "semifinal";
                case FINAL -> "final";
            };
            List<HeatResults> heatResults = new ArrayList<>();
            for (Heat h : heats.findByStageIdOrderByIndexInStage(st.getId())) {
                List<ResultEntry> entries = results.findByHeatId(h.getId()).stream()
                        .map(r -> new ResultEntry(r.getAthlete().getId(), r.getLane(), r.getTimeMs(), r.getStatus()))
                        .toList();
                heatResults.add(new HeatResults(h.getIndexInStage(), entries));
            }
            history.add(new StageResults(engineName, heatResults));
        }
        return history;
    }

    private Map<Long, Athlete> athleteIndex(StageHistory history) {
        // соберём спортсменов всех завершённых этапов для разрешения id → Athlete
        Map<Long, Athlete> map = new LinkedHashMap<>();
        for (String stageName : List.of("prelim", "semifinal", "final")) {
            StageResults sr = history.get(stageName);
            if (sr == null) {
                continue;
            }
            for (HeatResults h : sr.heats()) {
                for (ResultEntry e : h.results()) {
                    map.computeIfAbsent(e.athleteId(),
                            id -> athletes.findById(id).orElseThrow(() -> notFound("Спортсмен", id)));
                }
            }
        }
        return map;
    }

    private int countNotStarted(Stage stage) {
        int count = 0;
        for (Heat h : heats.findByStageId(stage.getId())) {
            for (Result r : results.findByHeatId(h.getId())) {
                if (r.getStatus() == ResultStatus.NOT_STARTED) {
                    count++;
                }
            }
        }
        return count;
    }

    private int nextHeatNumber(Category cat) {
        return heats.maxNumberInCompetition(cat.getCompetition().getId()) + 1;
    }

    /** Базовое время старта этапа: дата+время дня категории (если задан), иначе null. */
    private Instant stageBaseStart(Category cat) {
        CompetitionDay day = cat.getCompetitionDay();
        if (day == null || day.getDayDate() == null || day.getStartTime() == null) {
            return null;
        }
        return day.getDayDate().atTime(day.getStartTime()).atZone(ZoneId.systemDefault()).toInstant();
    }

    private Instant scheduledAt(Instant base, Category cat, int order) {
        return base == null ? null : base.plusSeconds((long) order * cat.getDefaultIntervalSec());
    }

    private Competition competition(long id) {
        return competitions.findById(id).orElseThrow(() -> notFound("Соревнование", id));
    }

    private Category category(long id) {
        return categories.findById(id).orElseThrow(() -> notFound("Категория", id));
    }

    private void requireStatus(Category cat, CategoryStatus... allowed) {
        for (CategoryStatus s : allowed) {
            if (cat.getStatus() == s) {
                return;
            }
        }
        throw new SeedingException("Действие недоступно в статусе «" + cat.getStatus() + "».");
    }

    private static SeedingException notFound(String what, long id) {
        return new SeedingException(what + " не найдено: " + id);
    }
}
