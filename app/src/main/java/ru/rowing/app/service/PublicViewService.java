package ru.rowing.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rowing.app.domain.Category;
import ru.rowing.app.domain.CategoryStatus;
import ru.rowing.app.domain.Competition;
import ru.rowing.app.domain.CompetitionStatus;
import ru.rowing.app.domain.Heat;
import ru.rowing.app.domain.Result;
import ru.rowing.app.repo.CompetitionRepository;
import ru.rowing.app.repo.HeatRepository;
import ru.rowing.app.repo.ResultRepository;
import ru.rowing.app.web.view.Labels;
import ru.rowing.app.web.view.PublicViews.CompetitionSummary;
import ru.rowing.app.web.view.PublicViews.CompetitionView;
import ru.rowing.app.web.view.PublicViews.DayView;
import ru.rowing.app.web.view.PublicViews.HeatCard;
import ru.rowing.app.web.view.PublicViews.HeatView;
import ru.rowing.app.web.view.PublicViews.ResultRow;
import ru.rowing.app.web.view.PublicViews.ResultSearchRow;
import ru.rowing.app.util.TimeFormat;
import ru.rowing.seeding.runtime.ResultStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Чтение данных для публичной части (раздел 7.1 ТЗ). Все DTO готовятся внутри транзакции,
 * т.к. open-in-view отключён. Протокол показывается только начиная с PROTOCOL_FORMED (раздел 6).
 */
@Service
public class PublicViewService {

    /** Категории, чей протокол уже можно показывать (всё, кроме DRAFT и REGISTRATION_OPEN). */
    private static final Collection<CategoryStatus> VISIBLE = EnumSet.complementOf(
            EnumSet.of(CategoryStatus.DRAFT, CategoryStatus.REGISTRATION_OPEN));

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private final CompetitionRepository competitions;
    private final ru.rowing.app.repo.CompetitionDayRepository days;
    private final HeatRepository heats;
    private final ResultRepository results;

    public PublicViewService(CompetitionRepository competitions, ru.rowing.app.repo.CompetitionDayRepository days,
                             HeatRepository heats, ResultRepository results) {
        this.competitions = competitions;
        this.days = days;
        this.heats = heats;
        this.results = results;
    }

    /** Главная: LIVE — наверху, затем по дате (включая назначенные). */
    @Transactional(readOnly = true)
    public List<CompetitionSummary> listCompetitions() {
        return competitions.findAll().stream()
                .sorted(Comparator
                        .comparing((Competition c) -> c.getStatus() == CompetitionStatus.LIVE ? 0 : 1)
                        .thenComparing(c -> c.getStartsAt(), Comparator.nullsLast(Comparator.naturalOrder())))
                .map(c -> new CompetitionSummary(c.getId(), c.getName(),
                        Labels.competitionStatus(c.getStatus()),
                        c.getStatus() == CompetitionStatus.LIVE,
                        formatInstant(c.getStartsAt())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<CompetitionView> getCompetition(long id) {
        return competitions.findById(id).map(c -> {
            // Заезды группируем по дню; внутри дня — по номеру (он же порядок по времени).
            Map<Long, List<Heat>> byDay = new java.util.HashMap<>();
            for (Heat h : heats.findVisibleByCompetition(id, VISIBLE)) {
                Long dayId = h.getCompetitionDay() != null ? h.getCompetitionDay().getId() : null;
                byDay.computeIfAbsent(dayId, k -> new ArrayList<>()).add(h);
            }

            List<DayView> dayViews = new ArrayList<>();
            for (var day : days.findByCompetitionIdOrderByOrdinal(id)) {
                List<Heat> dayHeats = byDay.remove(day.getId());
                if (dayHeats != null && !dayHeats.isEmpty()) {
                    dayViews.add(new DayView(dayTitle(day), cards(dayHeats)));
                }
            }
            // Заезды без привязки к дню (например, сформированные движком без дней) — одной таблицей.
            List<Heat> noDay = byDay.get(null);
            if (noDay != null && !noDay.isEmpty()) {
                dayViews.add(new DayView("Заезды", cards(noDay)));
            }

            return new CompetitionView(c.getId(), c.getName(), c.getDescription(),
                    Labels.competitionStatus(c.getStatus()), dayViews);
        });
    }

    private List<HeatCard> cards(List<Heat> heatList) {
        return heatList.stream()
                .sorted(Comparator.comparingInt(Heat::getNumber))
                .map(this::toHeatCard)
                .toList();
    }

    private static String dayTitle(ru.rowing.app.domain.CompetitionDay day) {
        String date = day.getDayDate() != null
                ? DateTimeFormatter.ofPattern("dd.MM.yyyy").format(day.getDayDate()) : "";
        return "День " + day.getOrdinal() + (date.isEmpty() ? "" : " · " + date);
    }

    @Transactional(readOnly = true)
    public Optional<HeatView> getHeat(long heatId) {
        return heats.findById(heatId)
                .filter(h -> VISIBLE.contains(h.getStage().getCategory().getStatus()))
                .map(this::toHeatView);
    }

    @Transactional(readOnly = true)
    public List<ResultSearchRow> search(String athlete, String coach, String category,
                                        String region, String school) {
        return results.search(norm(athlete), norm(coach), norm(category),
                        norm(region), norm(school), VISIBLE).stream()
                .map(this::toSearchRow)
                .toList();
    }

    // --- маппинг -------------------------------------------------------------

    private HeatCard toHeatCard(Heat h) {
        Category c = h.getStage().getCategory();
        List<Result> rows = results.findByHeatId(h.getId());
        boolean run = rows.stream().anyMatch(r -> r.getStatus() != ResultStatus.NOT_STARTED);
        return new HeatCard(h.getId(), h.getNumber(), formatTime(h.getScheduledStart()),
                Labels.boatClass(c.getBoatClass()), c.getDistanceM(), c.getName(),
                Labels.stage(h), h.getAdvancement(), run, rankResults(rows, run));
    }

    private HeatView toHeatView(Heat h) {
        Category c = h.getStage().getCategory();
        Competition comp = c.getCompetition();
        List<Result> rows = results.findByHeatId(h.getId());
        boolean run = rows.stream().anyMatch(r -> r.getStatus() != ResultStatus.NOT_STARTED);
        return new HeatView(h.getId(), h.getNumber(), comp.getId(), comp.getName(),
                c.getName(), Labels.stage(h), run, rankResults(rows, run));
    }

    /**
     * Сортировка результатов заезда (раздел 7.1, п.3): не пройден — по дорожке;
     * пройден — по времени, затем DNF/DNS/DSQ в конце по дорожке. Место — только у финишёров.
     */
    private List<ResultRow> rankResults(List<Result> rows, boolean run) {
        List<ResultRow> out = new ArrayList<>();
        if (!run) {
            rows.stream()
                    .sorted(Comparator.comparingInt(Result::getLane))
                    .forEach(r -> out.add(toResultRow(r, null)));
            return out;
        }
        List<Result> finishers = rows.stream()
                .filter(r -> r.getStatus() == ResultStatus.OK && r.getTimeMs() != null)
                .sorted(Comparator.comparingLong(Result::getTimeMs))
                .toList();
        int place = 1;
        for (Result r : finishers) {
            out.add(toResultRow(r, place++));
        }
        rows.stream()
                .filter(r -> !(r.getStatus() == ResultStatus.OK && r.getTimeMs() != null))
                .sorted(Comparator.comparingInt(Result::getLane))
                .forEach(r -> out.add(toResultRow(r, null)));
        return out;
    }

    private ResultRow toResultRow(Result r, Integer place) {
        var a = r.getAthlete();
        String coach = a.getCoach() != null ? a.getCoach().getFullName() : "";
        String statusLabel = r.getStatus() == ResultStatus.OK ? TimeFormat.format(r.getTimeMs())
                : Labels.resultStatus(r.getStatus());
        return new ResultRow(place, r.getLane(), a.getFullName(), a.getBirthYear(), a.getRegion(),
                a.getSportSchool(), coach, TimeFormat.format(r.getTimeMs()), statusLabel);
    }

    private ResultSearchRow toSearchRow(Result r) {
        Heat h = r.getHeat();
        Category c = h.getStage().getCategory();
        Competition comp = c.getCompetition();
        var a = r.getAthlete();
        return new ResultSearchRow(comp.getId(), comp.getName(), c.getName(), h.getId(), h.getNumber(),
                Labels.stage(h), a.getFullName(), a.getRegion(), a.getSportSchool(),
                a.getCoach() != null ? a.getCoach().getFullName() : "",
                TimeFormat.format(r.getTimeMs()), Labels.resultStatus(r.getStatus()));
    }

    private static String formatInstant(Instant instant) {
        return instant == null ? "" : DATE_TIME.format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }

    private static String formatTime(Instant instant) {
        return instant == null ? "" : TIME.format(instant);
    }

    /** Нормализует фильтр: null/пробелы → "" (пустой фильтр игнорируется в запросе). */
    private static String norm(String s) {
        return s == null ? "" : s.trim();
    }
}
