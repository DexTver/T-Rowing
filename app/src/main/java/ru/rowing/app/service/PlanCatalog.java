package ru.rowing.app.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import ru.rowing.app.domain.SeedingPlanFile;
import ru.rowing.app.repo.SeedingPlanRepository;
import ru.rowing.seeding.GridValidator;
import ru.rowing.seeding.PlanRepository;
import ru.rowing.seeding.SeedingEngine;
import ru.rowing.seeding.SeedingException;
import ru.rowing.seeding.ValidationResult;
import ru.rowing.seeding.model.Plan;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Каталог планов сеток A–N и единый экземпляр движка. Источник плана: встроенный ресурс
 * {@code seeding-engine}, при наличии переопределяется загруженным через админку файлом из БД
 * (раздел 7.4). Граница применимости — {@link #MIN_PLAN}..{@link #MAX_PLAN} участников (раздел 8.2).
 */
@Component
public class PlanCatalog {

    public static final int MIN_PLAN = 10;
    public static final int MAX_PLAN = 162;
    public static final String[] LETTERS = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q"
    };

    /** Статус плана для админки: источник, валидность, нарушения, диапазон участников. */
    public record PlanStatus(String letter, String source, boolean valid,
                             List<String> violations, Integer min, Integer max) {
    }

    /** Особый план для 10–18 экипажей: жеребьёвка сразу в 2 полуфинала (вне общего покрытия A–Q). */
    public static final String ALT = "A-alt";
    public static final int ALT_MIN = 10;
    public static final int ALT_MAX = 18;

    private final SeedingEngine engine = new SeedingEngine();
    private final SeedingPlanRepository store;
    private final Map<String, Plan> plans = new LinkedHashMap<>();
    private final Map<String, String> source = new LinkedHashMap<>();
    private Plan planAAlt;

    public PlanCatalog(SeedingPlanRepository store) {
        this.store = store;
    }

    @PostConstruct
    public void reload() {
        plans.clear();
        source.clear();
        for (String letter : LETTERS) {
            plans.put(letter, PlanRepository.fromClasspath("/seeding/plan_" + letter + ".json"));
            source.put(letter, "встроенный");
        }
        planAAlt = PlanRepository.fromClasspath("/seeding/plan_A-alt.json");
        for (SeedingPlanFile file : store.findAll()) {
            try {
                Plan parsed = PlanRepository.parse(file.getJson());
                if (ALT.equals(file.getLetter())) {
                    planAAlt = parsed; // особый план не входит в общее покрытие A–Q
                } else {
                    plans.put(file.getLetter(), parsed);
                    source.put(file.getLetter(), "загружен");
                }
            } catch (SeedingException e) {
                // повреждённый файл в БД не должен ронять старт — остаётся встроенный план
                source.put(file.getLetter(), "ошибка БД: " + e.getMessage());
            }
        }
    }

    public SeedingEngine engine() {
        return engine;
    }

    /** План для числа участников N (10..162). Бросает понятную ошибку вне диапазона. */
    public Plan planForCount(int n) {
        if (n > MAX_PLAN) {
            throw new SeedingException("Слишком много участников (" + n + "): система отбора рассчитана до " + MAX_PLAN);
        }
        return plans.values().stream()
                .filter(p -> p.participants() != null && p.participants().contains(n))
                .findFirst()
                .orElseThrow(() -> new SeedingException("Нет плана сетки для " + n + " участников"));
    }

    /** Буква плана для N участников (10..162); {@code null}, если вне диапазона (без исключения). */
    public String planLetterForCount(int n) {
        if (n < MIN_PLAN || n > MAX_PLAN) {
            return null;
        }
        return plans.values().stream()
                .filter(p -> p.participants() != null && p.participants().contains(n))
                .map(Plan::plan)
                .findFirst()
                .orElse(null);
    }

    /** Варианты посева плана (альтернативные раскладки полуфинала одного плана), например ["1","2"]. */
    public List<String> variantOptions(String letter) {
        Plan p = plans.get(letter);
        if (p == null || p.stage("semifinal") == null || p.stage("semifinal").variants() == null) {
            return List.of();
        }
        return p.stage("semifinal").variants().keySet().stream().sorted().toList();
    }

    /** План по сохранённой букве (для воспроизводимого формирования следующих этапов). */
    public Plan planByLetter(String letter) {
        if (ALT.equals(letter)) {
            return planAAlt;
        }
        Plan plan = plans.get(letter);
        if (plan == null) {
            throw new SeedingException("Неизвестный план сетки: " + letter);
        }
        return plan;
    }

    /** План по умолчанию при создании: для 10–18 — {@value #ALT} (наш уровень), иначе стандартный по N. */
    public String defaultPlanForCount(int n) {
        if (n >= ALT_MIN && n <= ALT_MAX) {
            return ALT;
        }
        return planLetterForCount(n);
    }

    /** Планы, среди которых судья может выбрать для данного N (для 10–18 — {@value #ALT} и стандартный A). */
    public List<String> planChoicesForCount(int n) {
        if (n >= ALT_MIN && n <= ALT_MAX) {
            return List.of(ALT, planLetterForCount(n));
        }
        String std = planLetterForCount(n);
        return std == null ? List.of() : List.of(std);
    }

    public List<Plan> all() {
        return List.copyOf(plans.values());
    }

    /**
     * Загрузка/замена файла сетки через админку: JSON валидируется (инварианты 8.7) и только при
     * успехе сохраняется в БД и применяется. Иначе — исключение со списком нарушений.
     */
    public void replace(String letter, String json) {
        Plan plan = PlanRepository.parse(json);
        if (!letter.equals(plan.plan())) {
            throw new SeedingException("Буква плана в файле (" + plan.plan() + ") не совпадает с выбранной (" + letter + ").");
        }
        ValidationResult result = GridValidator.validate(plan);
        result.throwIfInvalid();

        SeedingPlanFile file = store.findByLetter(letter).orElseGet(SeedingPlanFile::new);
        file.setLetter(letter);
        file.setJson(json);
        file.setUpdatedAt(Instant.now());
        store.save(file);

        plans.put(letter, plan);
        source.put(letter, "загружен");
    }

    /** Статусы всех планов для админки (инварианты каждого + источник). */
    public List<PlanStatus> statuses() {
        List<PlanStatus> list = new ArrayList<>();
        for (String letter : LETTERS) {
            Plan p = plans.get(letter);
            ValidationResult vr = GridValidator.validate(p);
            list.add(new PlanStatus(letter, source.getOrDefault(letter, "?"), vr.isValid(), vr.violations(),
                    p.participants() != null ? p.participants().min() : null,
                    p.participants() != null ? p.participants().max() : null));
        }
        return list;
    }

    /** Нарушения покрытия диапазона 10..162 по всему набору (инвариант 8.7 №6). */
    public List<String> coverageViolations() {
        return GridValidator.validateCoverage(all()).violations();
    }
}
