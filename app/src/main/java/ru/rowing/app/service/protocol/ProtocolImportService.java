package ru.rowing.app.service.protocol;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rowing.app.domain.Athlete;
import ru.rowing.app.domain.Category;
import ru.rowing.app.domain.CategoryStatus;
import ru.rowing.app.domain.Coach;
import ru.rowing.app.domain.Competition;
import ru.rowing.app.domain.CompetitionDay;
import ru.rowing.app.domain.CompetitionStatus;
import ru.rowing.app.domain.Heat;
import ru.rowing.app.domain.Result;
import ru.rowing.app.domain.Stage;
import ru.rowing.app.domain.StageType;
import ru.rowing.app.repo.AthleteRepository;
import ru.rowing.app.repo.CategoryRepository;
import ru.rowing.app.repo.CoachRepository;
import ru.rowing.app.repo.CompetitionDayRepository;
import ru.rowing.app.repo.CompetitionRepository;
import ru.rowing.app.repo.HeatRepository;
import ru.rowing.app.repo.ResultRepository;
import ru.rowing.app.repo.StageRepository;
import ru.rowing.app.web.view.Labels;
import ru.rowing.seeding.SeedingException;
import ru.rowing.seeding.runtime.ResultStatus;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Импорт соревнования из реального стартового протокола Excel.
 * <p>Файл, как правило, имеет лист «база» (картотека спортсменов: Номер, Фамилия, Имя, год, разряд,
 * субъект, организация, тренер) и лист протокола, где в колонке B стоит <b>Номер</b> спортсмена, а имя
 * подтягивается формулой {@code VLOOKUP} из базы. Поэтому имена берём по номеру из базы (а не из формулы),
 * а всю базу загружаем в БД. Если базы нет — имя берём из текста протокола (без формул).
 */
@Service
public class ProtocolImportService {

    public record ImportResult(long competitionId, int days, int categories, int heats,
                               int athletes, int baseAthletes, List<String> warnings) {
    }

    private static final Pattern RU_DATE = Pattern.compile("(\\d{1,2})\\s+([А-Яа-я]+)\\s+(\\d{4})");
    private static final Pattern NUM_DATE = Pattern.compile("(\\d{1,2})[.](\\d{1,2})[.](\\d{4})");
    private static final Pattern YEAR = Pattern.compile("(19|20)\\d{2}");
    private static final Map<String, Integer> MONTHS = months();

    private final CompetitionRepository competitions;
    private final CompetitionDayRepository days;
    private final CategoryRepository categories;
    private final AthleteRepository athletes;
    private final CoachRepository coaches;
    private final StageRepository stages;
    private final HeatRepository heats;
    private final ResultRepository results;

    public ProtocolImportService(CompetitionRepository competitions, CompetitionDayRepository days,
                                 CategoryRepository categories, AthleteRepository athletes, CoachRepository coaches,
                                 StageRepository stages, HeatRepository heats, ResultRepository results) {
        this.competitions = competitions;
        this.days = days;
        this.categories = categories;
        this.athletes = athletes;
        this.coaches = coaches;
        this.stages = stages;
        this.heats = heats;
        this.results = results;
    }

    @Transactional
    public ImportResult importFrom(InputStream xlsx, String fallbackName) {
        State st = new State();
        st.fallbackName = (fallbackName == null || fallbackName.isBlank())
                ? "Импортированное соревнование" : fallbackName;

        try (XSSFWorkbook wb = new XSSFWorkbook(xlsx)) {
            Sheet baza = findBaza(wb);
            if (baza != null) {
                importBase(baza, st);
            }
            Sheet protocol = findProtocol(wb, baza);
            if (protocol == null) {
                throw new SeedingException("Не найден лист со стартовым протоколом (строки «N з-д …»).");
            }
            parseProtocol(protocol, st);
        } catch (IOException e) {
            throw new SeedingException("Не удалось прочитать Excel: " + e.getMessage());
        }

        if (st.competition == null) {
            throw new SeedingException("В файле не найдено ни одного заезда — это не стартовый протокол?");
        }
        return new ImportResult(st.competition.getId(), st.dayOrdinal, st.categoryByKey.size(),
                st.heatCount, st.athleteByKey.size() + st.bibToAthlete.size(),
                st.bibToAthlete.size(), st.warnings);
    }

    // --- лист «база» -----------------------------------------------------------

    private void importBase(Sheet baza, State st) {
        for (int r = 1; r <= baza.getLastRowNum(); r++) {
            Row row = baza.getRow(r);
            if (row == null) {
                continue;
            }
            Long bib = num(row.getCell(0));
            String last = str(row.getCell(1));
            String first = str(row.getCell(2));
            String name = (last + " " + first).trim();
            if (bib == null || name.isEmpty()) {
                continue;
            }
            if (st.bibToAthlete.containsKey(bib)) {
                continue;
            }
            Athlete a = new Athlete();
            a.setExtNumber(bib);
            a.setFullName(name);
            Integer year = year(row.getCell(3));
            a.setBirthYear(year != null ? year : 0);
            a.setRank(blankNull(str(row.getCell(4))));
            a.setRegion(blankNull(str(row.getCell(5))));
            a.setSportSchool(blankNull(str(row.getCell(6))));
            a.setCoach(coachFor(st, str(row.getCell(7))));
            st.bibToAthlete.put(bib, athletes.save(a));
        }
    }

    // --- лист протокола --------------------------------------------------------

    private void parseProtocol(Sheet sheet, State st) {
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            String a = str(row.getCell(0));
            String title = rowTitle(row);
            if (title != null) {
                startDay(st, sheet, r, title);
            } else if (ProtocolHeader.isHeader(a)) {
                handleHeader(st, a);
            } else {
                handleDataRow(st, row);
            }
        }
    }

    private void startDay(State st, Sheet sheet, int rowIdx, String title) {
        if (st.competition == null) {
            st.competition = new Competition();
            st.competition.setName(title);
            st.competition.setStatus(CompetitionStatus.SCHEDULED);
            st.competition = competitions.save(st.competition);
        }
        st.dayOrdinal++;
        CompetitionDay day = new CompetitionDay();
        day.setCompetition(st.competition);
        day.setOrdinal(st.dayOrdinal);
        LocalDate date = findDateNear(sheet, rowIdx).orElse(LocalDate.now().plusDays(st.dayOrdinal - 1L));
        day.setDayDate(date);
        st.currentDay = days.save(day);
        if (st.competition.getStartsAt() == null) {
            st.competition.setStartsAt(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        st.currentHeat = null;
    }

    private void handleHeader(State st, String raw) {
        Optional<ProtocolHeader.Parsed> parsedOpt = ProtocolHeader.parse(raw);
        if (parsedOpt.isEmpty()) {
            st.warnings.add("Не разобран заголовок заезда: «" + raw.trim() + "»");
            return;
        }
        ensureCompetition(st);
        ProtocolHeader.Parsed p = parsedOpt.get();

        Category category = st.categoryByKey.computeIfAbsent(p.disciplineKey(), k -> {
            Category c = new Category();
            c.setCompetition(st.competition);
            c.setCompetitionDay(st.currentDay);
            c.setName(Labels.boatClass(p.boatClass()) + " " + p.distance() + " м " + p.categoryText());
            c.setBoatClass(p.boatClass());
            c.setGender(p.gender());
            c.setDistanceM(p.distance());
            c.setStatus(CategoryStatus.PROTOCOL_FORMED);
            return categories.save(c);
        });

        Stage stage = st.stageByKey.computeIfAbsent(category.getId() + "/" + p.stage(), k -> {
            Stage s = new Stage();
            s.setCategory(category);
            s.setType(p.stage());
            s.setOrdinal(stageOrdinal(p.stage()));
            return stages.save(s);
        });

        int indexInStage = p.stageIndex() != null
                ? p.stageIndex()
                : st.stageHeatCount.getOrDefault(stage.getId(), 0) + 1;
        st.stageHeatCount.merge(stage.getId(), 1, Integer::sum);

        Heat heat = new Heat();
        heat.setStage(stage);
        heat.setCompetitionDay(st.currentDay);
        heat.setNumber(p.fileNumber()); // номер как в протоколе (нумерация по дням)
        heat.setIndexInStage(indexInStage);
        heat.setFinalLetter(p.stage() == StageType.FINAL ? "A" : null);
        heat.setAdvancement(p.advancement());
        heat.setScheduledStart(scheduledStart(st.currentDay, p.time()));
        st.currentHeat = heats.save(heat);
        st.heatCount++;
    }

    private void handleDataRow(State st, Row row) {
        if (st.currentHeat == null) {
            return;
        }
        int lane = laneOf(row.getCell(0));
        if (lane < 1 || lane > 12) {
            return;
        }
        Athlete athlete = resolveAthlete(st, row);
        if (athlete == null) {
            return; // пустая дорожка или нераспознанные данные
        }
        Result res = new Result();
        res.setHeat(st.currentHeat);
        res.setAthlete(athlete);
        res.setLane(lane);
        res.setStatus(ResultStatus.NOT_STARTED);
        results.save(res);
    }

    /** Спортсмен дорожки: сначала по Номеру (колонка B) из базы, иначе по тексту протокола. */
    private Athlete resolveAthlete(State st, Row row) {
        Long bib = num(row.getCell(1));
        if (bib != null) {
            Athlete fromBase = st.bibToAthlete.get(bib);
            if (fromBase != null) {
                return fromBase;
            }
        }
        String name = personName(row.getCell(2));
        if (name.isEmpty()) {
            return null;
        }
        Integer year = year(row.getCell(3));
        String coachName = str(row.getCell(4));
        return st.athleteByKey.computeIfAbsent(name.toLowerCase() + "|" + year, k -> {
            Athlete x = new Athlete();
            x.setExtNumber(bib);
            x.setFullName(name);
            x.setBirthYear(year != null ? year : 0);
            x.setCoach(coachFor(st, coachName));
            return athletes.save(x);
        });
    }

    private Coach coachFor(State st, String name) {
        String n = blank(name);
        if (n.isEmpty()) {
            return null;
        }
        return st.coachByName.computeIfAbsent(n.toLowerCase(), k -> {
            Coach c = new Coach();
            c.setFullName(n);
            return coaches.save(c);
        });
    }

    private void ensureCompetition(State st) {
        if (st.competition == null) {
            st.competition = new Competition();
            st.competition.setName(st.fallbackName);
            st.competition.setStatus(CompetitionStatus.SCHEDULED);
            st.competition = competitions.save(st.competition);
        }
        if (st.currentDay == null) {
            st.dayOrdinal++;
            CompetitionDay day = new CompetitionDay();
            day.setCompetition(st.competition);
            day.setOrdinal(st.dayOrdinal);
            day.setDayDate(LocalDate.now());
            st.currentDay = days.save(day);
        }
    }

    // --- выбор листов ----------------------------------------------------------

    private Sheet findBaza(XSSFWorkbook wb) {
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Sheet s = wb.getSheetAt(i);
            if (s.getSheetName().trim().toLowerCase().contains("база")) {
                return s;
            }
            Row h = s.getRow(0);
            if (h != null && str(h.getCell(0)).equalsIgnoreCase("Номер")
                    && str(h.getCell(1)).toLowerCase().contains("фамил")) {
                return s;
            }
        }
        return null;
    }

    private Sheet findProtocol(XSSFWorkbook wb, Sheet baza) {
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Sheet s = wb.getSheetAt(i);
            if (s == baza) {
                continue;
            }
            int scan = Math.min(s.getLastRowNum(), 60);
            for (int r = 0; r <= scan; r++) {
                Row row = s.getRow(r);
                if (row != null && ProtocolHeader.isHeader(str(row.getCell(0)))) {
                    return s;
                }
            }
        }
        return null;
    }

    private String rowTitle(Row row) {
        for (int c = 0; c <= 6; c++) {
            String v = str(row.getCell(c));
            if (v.toLowerCase().contains("соревнован") && !ProtocolHeader.isHeader(v)) {
                return v.replaceAll("\\s+", " ").trim();
            }
        }
        return null;
    }

    // --- чтение ячеек (безопасно к формулам) -----------------------------------

    /** Текст ячейки; для формул берём кэшированный результат, а не текст формулы. */
    private String str(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            case NUMERIC -> numberToString(cell.getNumericCellValue());
            case FORMULA -> switch (cell.getCachedFormulaResultType()) {
                case STRING -> cell.getStringCellValue().trim();
                case NUMERIC -> numberToString(cell.getNumericCellValue());
                default -> "";
            };
            default -> "";
        };
    }

    /** Имя спортсмена из текстовой ячейки; отбрасывает формулы/прочерки. */
    private String personName(Cell cell) {
        String s = str(cell);
        if (s.startsWith("=") || s.toUpperCase().contains("VLOOKUP") || s.equals("-")) {
            return "";
        }
        return s;
    }

    private Long num(Cell cell) {
        if (cell == null) {
            return null;
        }
        double d;
        switch (cell.getCellType()) {
            case NUMERIC -> d = cell.getNumericCellValue();
            case FORMULA -> {
                if (cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                    d = cell.getNumericCellValue();
                } else {
                    return parseLong(str(cell));
                }
            }
            case STRING -> {
                return parseLong(cell.getStringCellValue().trim());
            }
            default -> {
                return null;
            }
        }
        return d == Math.floor(d) ? (long) d : null;
    }

    private int laneOf(Cell cell) {
        Long n = num(cell);
        return n == null ? -1 : n.intValue();
    }

    private Integer year(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC || (cell.getCellType() == CellType.FORMULA
                && cell.getCachedFormulaResultType() == CellType.NUMERIC)) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().getYear();
            }
            double d = cell.getNumericCellValue();
            if (d >= 1900 && d <= 2100) {
                return (int) d;
            }
            if (d > 10000) {
                return DateUtil.getLocalDateTime(d).getYear();
            }
            return null;
        }
        String s = str(cell);
        Matcher nd = NUM_DATE.matcher(s);
        if (nd.find()) {
            return Integer.parseInt(nd.group(3));
        }
        Matcher y = YEAR.matcher(s);
        return y.find() ? Integer.parseInt(y.group()) : null;
    }

    private Instant scheduledStart(CompetitionDay day, String time) {
        if (day == null || day.getDayDate() == null || time == null) {
            return null;
        }
        try {
            LocalTime t = LocalTime.parse(time.length() == 4 ? "0" + time : time);
            return LocalDateTime.of(day.getDayDate(), t).atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception e) {
            return null;
        }
    }

    private Optional<LocalDate> findDateNear(Sheet sheet, int fromRow) {
        for (int r = fromRow; r <= Math.min(fromRow + 4, sheet.getLastRowNum()); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            for (int c = 0; c <= 8; c++) {
                Cell cell = row.getCell(c);
                if (cell == null) {
                    continue;
                }
                if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                    return Optional.of(cell.getLocalDateTimeCellValue().toLocalDate());
                }
                String s = str(cell);
                Matcher m = RU_DATE.matcher(s);
                if (m.find()) {
                    Integer mon = MONTHS.get(m.group(2).toLowerCase());
                    if (mon != null) {
                        return Optional.of(LocalDate.of(
                                Integer.parseInt(m.group(3)), mon, Integer.parseInt(m.group(1))));
                    }
                }
                Matcher nd = NUM_DATE.matcher(s);
                if (nd.find()) {
                    return Optional.of(LocalDate.of(Integer.parseInt(nd.group(3)),
                            Integer.parseInt(nd.group(2)), Integer.parseInt(nd.group(1))));
                }
            }
        }
        return Optional.empty();
    }

    private static String numberToString(double d) {
        return d == Math.floor(d) ? Long.toString((long) d) : Double.toString(d);
    }

    private static Long parseLong(String s) {
        try {
            return s.isEmpty() ? null : (long) Double.parseDouble(s.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String blank(String s) {
        return s == null ? "" : s.trim();
    }

    private static String blankNull(String s) {
        String t = blank(s);
        return t.isEmpty() ? null : t;
    }

    private static int stageOrdinal(StageType type) {
        return switch (type) {
            case PRELIM -> 1;
            case SEMIFINAL -> 2;
            case FINAL -> 3;
        };
    }

    private static Map<String, Integer> months() {
        Map<String, Integer> m = new HashMap<>();
        String[] names = {"января", "февраля", "марта", "апреля", "мая", "июня",
                "июля", "августа", "сентября", "октября", "ноября", "декабря"};
        for (int i = 0; i < names.length; i++) {
            m.put(names[i], i + 1);
        }
        return m;
    }

    private static final class State {
        String fallbackName;
        Competition competition;
        CompetitionDay currentDay;
        Heat currentHeat;
        int dayOrdinal;
        int heatCount;
        final Map<String, Category> categoryByKey = new HashMap<>();
        final Map<String, Stage> stageByKey = new HashMap<>();
        final Map<Long, Integer> stageHeatCount = new HashMap<>();
        final Map<Long, Athlete> bibToAthlete = new HashMap<>();
        final Map<String, Athlete> athleteByKey = new HashMap<>();
        final Map<String, Coach> coachByName = new HashMap<>();
        final List<String> warnings = new ArrayList<>();
    }
}
