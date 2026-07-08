package ru.rowing.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rowing.app.domain.Category;
import ru.rowing.app.domain.CategoryStatus;
import ru.rowing.app.domain.Competition;
import ru.rowing.app.domain.CompetitionDay;
import ru.rowing.app.domain.Heat;
import ru.rowing.app.domain.Result;
import ru.rowing.app.domain.Stage;
import ru.rowing.app.domain.StageType;
import ru.rowing.app.repo.CategoryRepository;
import ru.rowing.app.repo.CompetitionDayRepository;
import ru.rowing.app.repo.CompetitionRepository;
import ru.rowing.app.repo.EntryRepository;
import ru.rowing.app.repo.HeatRepository;
import ru.rowing.app.repo.ResultRepository;
import ru.rowing.app.repo.StageRepository;
import ru.rowing.app.util.TimeFormat;
import ru.rowing.app.web.view.JudgeViews.CatRow;
import ru.rowing.app.web.view.JudgeViews.CategoryView;
import ru.rowing.app.web.view.JudgeViews.CompetitionRow;
import ru.rowing.app.web.view.JudgeViews.DayRow;
import ru.rowing.app.web.view.JudgeViews.EntryRow;
import ru.rowing.app.web.view.JudgeViews.HeatBlock;
import ru.rowing.app.web.view.JudgeViews.HeatEntryView;
import ru.rowing.app.web.view.JudgeViews.LaneRow;
import ru.rowing.app.web.view.JudgeViews.ManageView;
import ru.rowing.app.web.view.JudgeViews.StageBlock;
import ru.rowing.app.web.view.Labels;
import ru.rowing.seeding.runtime.ResultStatus;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Чтение данных судейских страниц (DTO готовятся в транзакции, open-in-view выключен). */
@Service
public class JudgeViewService {

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter T = DateTimeFormatter.ofPattern("HH:mm");

    private final CompetitionRepository competitions;
    private final CompetitionDayRepository days;
    private final CategoryRepository categories;
    private final EntryRepository entries;
    private final StageRepository stages;
    private final HeatRepository heats;
    private final ResultRepository results;
    private final PlanCatalog catalog;

    public JudgeViewService(CompetitionRepository competitions, CompetitionDayRepository days,
                            CategoryRepository categories, EntryRepository entries, StageRepository stages,
                            HeatRepository heats, ResultRepository results, PlanCatalog catalog) {
        this.competitions = competitions;
        this.days = days;
        this.categories = categories;
        this.entries = entries;
        this.stages = stages;
        this.heats = heats;
        this.results = results;
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public List<CompetitionRow> listCompetitions() {
        return competitions.findAll().stream()
                .sorted(Comparator.comparing(Competition::getId).reversed())
                .map(c -> new CompetitionRow(c.getId(), c.getName(), Labels.competitionStatus(c.getStatus())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ManageView> manage(long competitionId) {
        return competitions.findById(competitionId).map(c -> {
            List<DayRow> dayRows = days.findByCompetitionIdOrderByOrdinal(competitionId).stream()
                    .map(d -> new DayRow(d.getId(), d.getOrdinal(),
                            d.getDayDate() != null ? D.format(d.getDayDate()) : "",
                            d.getStartTime() != null ? T.format(d.getStartTime()) : ""))
                    .toList();
            List<CatRow> catRows = categories.findByCompetitionId(competitionId).stream()
                    .map(cat -> new CatRow(cat.getId(), cat.getName(), dayLabel(cat.getCompetitionDay()),
                            cat.getDistanceM(), Labels.categoryStatus(cat.getStatus())))
                    .toList();
            return new ManageView(c.getId(), c.getName(), c.getDescription(),
                    Labels.competitionStatus(c.getStatus()), dayRows, catRows);
        });
    }

    @Transactional(readOnly = true)
    public Optional<CategoryView> category(long categoryId) {
        return categories.findById(categoryId).map(cat -> {
            List<EntryRow> entryRows = entries.findByCategoryId(categoryId).stream()
                    .map(e -> new EntryRow(e.getAthlete().getFullName(), e.getAthlete().getBirthYear(),
                            e.getAthlete().getRegion(), e.getAthlete().getSportSchool()))
                    .toList();

            List<Stage> stageList = stages.findByCategoryIdOrderByOrdinal(categoryId);
            List<StageBlock> stageBlocks = stageList.stream().map(this::toStageBlock).toList();

            boolean allEntered = !stageList.isEmpty() && countNotStarted(stageList.getLast()) == 0;
            StageType latestType = stageList.isEmpty() ? null : stageList.getLast().getType();
            boolean canFormNext = allEntered && latestType != null && latestType != StageType.FINAL;
            String nextLabel = latestType == StageType.PRELIM ? "Сформировать полуфиналы"
                    : latestType == StageType.SEMIFINAL ? "Сформировать финалы" : null;

            List<String> variantOptions = cat.getPlan() != null ? catalog.variantOptions(cat.getPlan()) : List.of();
            // Нужно выбрать вариант, прежде чем формировать следующий этап (есть план и есть из чего выбрать).
            boolean needsVariant = canFormNext && !variantOptions.isEmpty() && cat.getActiveVariant() == null;

            CategoryStatus st = cat.getStatus();
            return new CategoryView(cat.getId(), cat.getCompetition().getId(), cat.getCompetition().getName(),
                    cat.getName(), Labels.categoryStatus(st), cat.getPlan(), cat.getActiveVariant(), variantOptions,
                    cat.isMassStart(), entryRows, stageBlocks,
                    st == CategoryStatus.DRAFT,
                    st == CategoryStatus.DRAFT || st == CategoryStatus.REGISTRATION_OPEN,
                    st == CategoryStatus.REGISTRATION_OPEN,
                    canFormNext, needsVariant, nextLabel);
        });
    }

    @Transactional(readOnly = true)
    public Optional<HeatEntryView> heatEntry(long heatId) {
        return heats.findById(heatId).map(h -> {
            Category cat = h.getStage().getCategory();
            return new HeatEntryView(h.getId(), h.getNumber(), Labels.stage(h),
                    cat.getId(), cat.getName(), laneRows(h));
        });
    }

    /** Заезды соревнования в диапазоне сквозных номеров — для печати протоколов (раздел 7.3, п.3). */
    @Transactional(readOnly = true)
    public List<HeatBlock> printRange(long competitionId, Integer from, Integer to) {
        return heats.findVisibleByCompetition(competitionId, List.of(CategoryStatus.values())).stream()
                .filter(h -> (from == null || h.getNumber() >= from) && (to == null || h.getNumber() <= to))
                .sorted(Comparator.comparingInt(Heat::getNumber))
                .map(this::toHeatBlock)
                .toList();
    }

    @Transactional(readOnly = true)
    public String competitionName(long competitionId) {
        return competitions.findById(competitionId).map(Competition::getName).orElse("");
    }

    // --- маппинг ---

    private StageBlock toStageBlock(Stage stage) {
        String label = switch (stage.getType()) {
            case PRELIM -> "Предварительный этап";
            case SEMIFINAL -> "Полуфиналы";
            case FINAL -> "Финалы";
        };
        List<HeatBlock> heatBlocks = heats.findByStageIdOrderByIndexInStage(stage.getId()).stream()
                .map(this::toHeatBlock)
                .toList();
        return new StageBlock(label, heatBlocks);
    }

    private HeatBlock toHeatBlock(Heat h) {
        List<Result> rs = results.findByHeatId(h.getId());
        boolean run = rs.stream().anyMatch(r -> r.getStatus() != ResultStatus.NOT_STARTED);
        return new HeatBlock(h.getId(), h.getNumber(), Labels.stage(h), run, laneRows(h));
    }

    private List<LaneRow> laneRows(Heat h) {
        return results.findByHeatId(h.getId()).stream()
                .sorted(Comparator.comparingInt(Result::getLane))
                .map(r -> new LaneRow(r.getLane(), r.getAthlete().getFullName(), laneValue(r)))
                .toList();
    }

    private static String laneValue(Result r) {
        if (r.getStatus() == ResultStatus.OK) {
            return TimeFormat.format(r.getTimeMs());
        }
        if (r.getStatus() == ResultStatus.NOT_STARTED) {
            return "";
        }
        return Labels.resultStatus(r.getStatus());
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

    private static String dayLabel(CompetitionDay day) {
        if (day == null) {
            return "—";
        }
        return "День " + day.getOrdinal() + (day.getDayDate() != null ? " (" + D.format(day.getDayDate()) + ")" : "");
    }
}
