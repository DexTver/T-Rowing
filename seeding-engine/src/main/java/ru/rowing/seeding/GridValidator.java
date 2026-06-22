package ru.rowing.seeding;

import ru.rowing.seeding.model.Assignment;
import ru.rowing.seeding.model.BestTimeSource;
import ru.rowing.seeding.model.Plan;
import ru.rowing.seeding.model.PlaceByTimeSource;
import ru.rowing.seeding.model.PlaceSource;
import ru.rowing.seeding.model.Source;
import ru.rowing.seeding.model.StageSpec;
import ru.rowing.seeding.model.Target;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Валидатор файла сетки (раздел 8.7 ТЗ). Прогоняется при загрузке/сохранении файла и в CI.
 * Возвращает список нарушений; пустой список означает, что файл валиден.
 */
public final class GridValidator {

    /** Покрываемый планами диапазон числа участников (раздел 8.7, инвариант 6). */
    public static final int COVERAGE_MIN = 10;
    public static final int COVERAGE_MAX = 135;

    private GridValidator() {
    }

    /** Проверка одного плана (инварианты 1–5). */
    public static ValidationResult validate(Plan plan) {
        List<String> violations = new ArrayList<>();
        String id = "план " + plan.plan();

        if (plan.lanes() <= 0) {
            violations.add(id + ": число дорожек должно быть положительным");
            return new ValidationResult(violations);
        }
        int lanes = plan.lanes();

        // Инвариант 3: размеры предв. заездов для каждого N диапазона дают сумму N и не превышают дорожек.
        validatePrelimSizes(plan, lanes, violations, id);

        if (plan.stages() != null) {
            for (StageSpec stage : plan.stages()) {
                validateStage(plan, stage, lanes, violations, id);
            }
        }
        return new ValidationResult(violations);
    }

    private static void validatePrelimSizes(Plan plan, int lanes, List<String> violations, String id) {
        if (plan.participants() == null || plan.prelims() == null) {
            return;
        }
        for (int n = plan.participants().min(); n <= plan.participants().max(); n++) {
            List<Integer> sizes;
            try {
                sizes = PrelimSizing.sizesFor(plan, n);
            } catch (SeedingException e) {
                violations.add(id + ", N=" + n + ": " + e.getMessage());
                continue;
            }
            int sum = sizes.stream().mapToInt(Integer::intValue).sum();
            if (sum != n) {
                violations.add(id + ", N=" + n + ": сумма размеров заездов " + sizes + " = " + sum + " ≠ " + n);
            }
            if (sizes.size() != plan.prelims().count()) {
                violations.add(id + ", N=" + n + ": число заездов " + sizes.size()
                        + " ≠ объявленного " + plan.prelims().count());
            }
            for (int size : sizes) {
                if (size > lanes) {
                    violations.add(id + ", N=" + n + ": заезд из " + size + " превышает " + lanes + " дорожек");
                }
            }
        }
    }

    private static void validateStage(Plan plan, StageSpec stage, int lanes, List<String> violations, String id) {
        if (stage.variants() == null || stage.variants().isEmpty()) {
            violations.add(id + ", этап " + stage.stage() + ": нет вариантов посева");
            return;
        }
        for (Map.Entry<String, List<Assignment>> e : stage.variants().entrySet()) {
            String ctx = id + ", этап " + stage.stage() + ", вариант " + e.getKey();
            validateVariant(stage, e.getValue(), lanes, violations, ctx);
        }
    }

    private static void validateVariant(StageSpec stage, List<Assignment> assignments,
                                        int lanes, List<String> violations, String ctx) {
        // Группируем назначения по целевому заезду.
        Map<String, List<Assignment>> byTargetHeat = new LinkedHashMap<>();
        Set<String> usedSourceSlots = new HashSet<>();

        for (Assignment a : assignments) {
            Target t = a.target();
            String heatKey = t.isFinal() ? "final " + t.finalLetter() : "п/ф " + t.heat();
            byTargetHeat.computeIfAbsent(heatKey, k -> new ArrayList<>()).add(a);

            // Инвариант 5: place/rank положительны и в физических пределах.
            validateSource(a.source(), lanes, violations, ctx);

            // Инвариант 2: каждый исходный слот «место P заезда H» используется не более одного раза.
            if (a.source() instanceof PlaceSource ps) {
                String slot = "место " + ps.place() + " заезда " + ps.from().stage() + "/" + ps.from().heat();
                if (!usedSourceSlots.add(slot)) {
                    violations.add(ctx + ": исходный слот «" + slot + "» используется более одного раза");
                }
            }
        }

        // Инвариант 1 (+4): для каждого целевого заезда дорожки 1..lanes заполнены ровно по разу.
        for (Map.Entry<String, List<Assignment>> e : byTargetHeat.entrySet()) {
            validateLaneCoverage(e.getKey(), e.getValue(), lanes, violations, ctx);
        }
    }

    private static void validateLaneCoverage(String heatKey, List<Assignment> assignments,
                                             int lanes, List<String> violations, String ctx) {
        Map<Integer, Integer> laneCount = new TreeMap<>();
        for (Assignment a : assignments) {
            laneCount.merge(a.target().lane(), 1, Integer::sum);
        }
        List<Integer> duplicates = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : laneCount.entrySet()) {
            if (e.getValue() > 1) {
                duplicates.add(e.getKey());
            }
        }
        if (!duplicates.isEmpty()) {
            violations.add(ctx + ", " + heatKey + ": дорожки заняты повторно: " + duplicates);
        }
        List<Integer> missing = new ArrayList<>();
        for (int lane = 1; lane <= lanes; lane++) {
            if (!laneCount.containsKey(lane)) {
                missing.add(lane);
            }
        }
        if (!missing.isEmpty()) {
            violations.add(ctx + ", " + heatKey + ": не заполнены дорожки: " + missing);
        }
        List<Integer> extra = laneCount.keySet().stream().filter(l -> l < 1 || l > lanes)
                .sorted().toList();
        if (!extra.isEmpty()) {
            violations.add(ctx + ", " + heatKey + ": дорожки вне диапазона 1.." + lanes
                    + " (10-я добавляется только в рантайме): " + extra);
        }
    }

    private static void validateSource(Source source, int lanes, List<String> violations, String ctx) {
        switch (source) {
            case PlaceSource ps -> {
                if (ps.place() < 1 || ps.place() > lanes) {
                    violations.add(ctx + ": место " + ps.place() + " вне диапазона 1.." + lanes);
                }
                if (ps.from() == null || ps.from().heat() < 1) {
                    violations.add(ctx + ": некорректная ссылка на заезд-источник");
                }
            }
            case BestTimeSource bt -> {
                if (bt.rank() < 1) {
                    violations.add(ctx + ": rank по времени должен быть ≥ 1");
                }
                if (bt.pool() == null || bt.pool().stage() == null) {
                    violations.add(ctx + ": не указан пул для отбора по времени");
                }
            }
            case PlaceByTimeSource pbt -> {
                if (pbt.place() < 1 || pbt.place() > lanes) {
                    violations.add(ctx + ": место " + pbt.place() + " вне диапазона 1.." + lanes);
                }
                if (pbt.rank() < 1) {
                    violations.add(ctx + ": rank по времени должен быть ≥ 1");
                }
                if (pbt.pool() == null || pbt.pool().stage() == null) {
                    violations.add(ctx + ": не указан пул для сквозного отбора по времени");
                }
            }
        }
    }

    /**
     * Инвариант 6: диапазоны участников планов не пересекаются и покрывают
     * {@value #COVERAGE_MIN}..{@value #COVERAGE_MAX} без дыр.
     */
    public static ValidationResult validateCoverage(List<Plan> plans) {
        List<String> violations = new ArrayList<>();
        List<Plan> sorted = new ArrayList<>(plans);
        sorted.removeIf(p -> p.participants() == null);
        sorted.sort(Comparator.comparingInt(p -> p.participants().min()));

        int expectedNext = COVERAGE_MIN;
        for (Plan p : sorted) {
            int min = p.participants().min();
            int max = p.participants().max();
            if (min > expectedNext) {
                violations.add("Не покрыт диапазон участников " + expectedNext + ".." + (min - 1));
            } else if (min < expectedNext) {
                violations.add("Пересечение диапазонов у плана " + p.plan()
                        + ": начинается с " + min + ", ожидалось " + expectedNext);
            }
            expectedNext = Math.max(expectedNext, max + 1);
        }
        if (expectedNext <= COVERAGE_MAX) {
            violations.add("Не покрыт диапазон участников " + expectedNext + ".." + COVERAGE_MAX);
        }
        return new ValidationResult(violations);
    }
}
