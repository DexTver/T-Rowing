package ru.rowing.seeding;

import ru.rowing.seeding.draw.HeatDraw;
import ru.rowing.seeding.draw.LaneAssignment;
import ru.rowing.seeding.draw.PrelimDraw;
import ru.rowing.seeding.draw.StageDraw;
import ru.rowing.seeding.model.Assignment;
import ru.rowing.seeding.model.BestTimeSource;
import ru.rowing.seeding.model.Plan;
import ru.rowing.seeding.model.PlaceByTimeSource;
import ru.rowing.seeding.model.PlaceSource;
import ru.rowing.seeding.model.Pool;
import ru.rowing.seeding.model.Source;
import ru.rowing.seeding.model.StageSpec;
import ru.rowing.seeding.model.Target;
import ru.rowing.seeding.runtime.HeatResults;
import ru.rowing.seeding.runtime.ResultEntry;
import ru.rowing.seeding.runtime.StageHistory;
import ru.rowing.seeding.runtime.StageResults;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * Движок жеребьёвки и формирования сеток (раздел 8 ТЗ). Чистая логика без БД и веба.
 * <ul>
 *   <li>{@link #drawPreliminaries} — случайное (по сохранённому зерну) распределение заявок по предв. заездам;</li>
 *   <li>{@link #directFinal} — особый случай N&lt;10: один финальный заезд со всеми (раздел 8.2);</li>
 *   <li>{@link #formNextStage} — детерминированный посев полуфиналов/финалов по файлу сетки.</li>
 * </ul>
 *
 * <p><b>Семантика отбора по времени.</b> Источники разрешаются фазами, чтобы пулы «по времени»
 * не пересекались (как требует первоисточник и {@code _meta.note} планов D–N):
 * <ol>
 *   <li>сперва все назначения «по месту» ({@code place});</li>
 *   <li>затем сквозной отбор «X из Y-ых» ({@code place_by_time}) — ранг абсолютен по всему пулу мест;</li>
 *   <li>затем «лучшие по времени» ({@code best_time}) по порядку финалов A→B→C — ранг отсчитывается
 *       от пула, <i>оставшегося</i> на момент данного финала (уже занятые исключаются).</li>
 * </ol>
 */
public final class SeedingEngine {

    public static final int OVERFLOW_LANE = 10;

    /**
     * Жеребьёвка предварительных заездов (раздел 8.2). Участники перемешиваются детерминированно
     * по {@code seed}, раскладываются по заездам согласно размерам плана, дорожки назначаются из перестановки.
     */
    public PrelimDraw drawPreliminaries(Plan plan, List<Long> participantIds, long seed) {
        int n = participantIds.size();
        List<Integer> sizes = PrelimSizing.sizesFor(plan, n);

        List<Long> shuffled = new ArrayList<>(participantIds);
        Collections.shuffle(shuffled, new Random(seed));

        List<HeatDraw> heats = new ArrayList<>();
        int cursor = 0;
        for (int h = 0; h < sizes.size(); h++) {
            int size = sizes.get(h);
            List<LaneAssignment> lanes = new ArrayList<>(size);
            // [ДОПУЩЕНИЕ] дорожки предв. заезда назначаются компактно 1..size в случайном порядке.
            for (int lane = 1; lane <= size; lane++) {
                lanes.add(new LaneAssignment(lane, shuffled.get(cursor++)));
            }
            heats.add(HeatDraw.semifinal(h + 1, lanes));
        }
        return new PrelimDraw(seed, heats);
    }

    /**
     * Особый случай N&lt;10 (раздел 8.2): система отбора не применяется,
     * формируется сразу финал A одним заездом со всеми участниками (дорожки — жеребьёвкой).
     */
    public StageDraw directFinal(List<Long> participantIds, long seed) {
        List<Long> shuffled = new ArrayList<>(participantIds);
        Collections.shuffle(shuffled, new Random(seed));
        List<LaneAssignment> lanes = new ArrayList<>();
        for (int i = 0; i < shuffled.size(); i++) {
            lanes.add(new LaneAssignment(i + 1, shuffled.get(i)));
        }
        return new StageDraw("final", List.of(HeatDraw.finalHeat("A", lanes)));
    }

    /**
     * Формирование следующего этапа посевом (раздел 8.2). Детерминировано: рандом не применяется.
     *
     * @param plan          план категории
     * @param activeVariant активный вариант посева (для финала игнорируется — берётся "default")
     * @param toStage       целевой этап: "semifinal" или "final"
     * @param history       результаты завершённых этапов
     * @param enabledFinals реально формируемые финалы (раздел 8.9); для полуфинала не используется
     */
    public StageDraw formNextStage(Plan plan, String activeVariant, String toStage,
                                   StageHistory history, Set<String> enabledFinals) {
        StageSpec stage = plan.stage(toStage);
        if (stage == null) {
            throw new SeedingException("В плане " + plan.plan() + " нет этапа " + toStage);
        }
        boolean toFinal = "final".equals(toStage);
        List<Assignment> assignments = stage.assignmentsFor(activeVariant);
        Map<String, Set<Long>> placeAdvanced = collectAdvancedByPlace(plan, activeVariant, history);

        Map<String, List<LaneAssignment>> byHeat = new LinkedHashMap<>();
        Set<Long> assigned = new HashSet<>();

        // Фаза 1 — назначения «по месту».
        for (Assignment a : assignments) {
            if (!(a.source() instanceof PlaceSource ps) || isDisabled(a.target(), toFinal, enabledFinals)) {
                continue;
            }
            resolvePlace(history, ps.from().stage(), ps.from().heat(), ps.place())
                    .ifPresent(e -> assign(byHeat, assigned, a.target(), e.athleteId()));
        }

        // Фаза 2 — «X из Y-ых» (place_by_time); ранг абсолютен по всему пулу мест.
        for (Assignment a : assignments) {
            if (!(a.source() instanceof PlaceByTimeSource pbt) || isDisabled(a.target(), toFinal, enabledFinals)) {
                continue;
            }
            List<ResultEntry> pool = placePool(history, pbt.pool().stage(), pbt.place());
            if (pbt.rank() <= pool.size()) {
                assign(byHeat, assigned, a.target(), pool.get(pbt.rank() - 1).athleteId());
            }
        }

        // Фаза 3 — «лучшие по времени» по порядку финалов; пул сужается уже занятыми.
        for (String group : bestTimeGroups(stage, toFinal, enabledFinals)) {
            resolveBestTimeGroup(assignments, toFinal, group, history, placeAdvanced, assigned, byHeat);
        }

        return buildStageDraw(toStage, stage, enabledFinals, byHeat);
    }

    // --- фаза 3: группа best_time одного финала (или весь полуфинальный переход) ----------

    private void resolveBestTimeGroup(List<Assignment> assignments, boolean toFinal, String group,
                                      StageHistory history, Map<String, Set<Long>> placeAdvanced,
                                      Set<Long> assigned, Map<String, List<LaneAssignment>> byHeat) {
        List<Assignment> bts = new ArrayList<>();
        for (Assignment a : assignments) {
            if (a.source() instanceof BestTimeSource && inGroup(a.target(), toFinal, group)) {
                bts.add(a);
            }
        }
        if (bts.isEmpty()) {
            return;
        }
        Pool pool = ((BestTimeSource) bts.getFirst().source()).pool();
        // Снимок пула на момент этого финала: финишёры − прошедшие по месту − уже занятые.
        List<ResultEntry> snapshot = timePool(history, pool, placeAdvanced, assigned);

        int maxRank = 0;
        String boundaryHeatKey = null;
        for (Assignment a : bts) {
            BestTimeSource bt = (BestTimeSource) a.source();
            if (bt.rank() <= snapshot.size()) {
                assign(byHeat, assigned, a.target(), snapshot.get(bt.rank() - 1).athleteId());
            }
            if (bt.rank() > maxRank) {
                maxRank = bt.rank();
                boundaryHeatKey = heatKey(a.target());
            }
        }

        // Тай-брейк (8.4): равенство времени на границе отбора — оба проходят, дополнительный на 10-ю дорожку.
        if (maxRank > 0 && snapshot.size() > maxRank
                && snapshot.get(maxRank).timeMs().longValue() == snapshot.get(maxRank - 1).timeMs().longValue()) {
            ResultEntry extra = snapshot.get(maxRank);
            byHeat.computeIfAbsent(boundaryHeatKey, k -> new ArrayList<>())
                    .add(new LaneAssignment(OVERFLOW_LANE, extra.athleteId()));
            assigned.add(extra.athleteId());
        }
    }

    // --- разрешение источников ------------------------------------------------------------

    private Optional<ResultEntry> resolvePlace(StageHistory history, String stage, int heat, int place) {
        StageResults sr = history.get(stage);
        if (sr == null) {
            return Optional.empty();
        }
        return sr.heat(heat).flatMap(h -> h.atPlace(place));
    }

    /** Пул «по времени»: финишёры пула без прошедших по месту (если задано) и без уже занятых; по возрастанию времени. */
    private List<ResultEntry> timePool(StageHistory history, Pool pool,
                                       Map<String, Set<Long>> placeAdvanced, Set<Long> assigned) {
        StageResults sr = history.get(pool.stage());
        if (sr == null) {
            return List.of();
        }
        Set<Long> excluded = new HashSet<>(assigned);
        if (pool.excludeAdvancedOrDefault()) {
            excluded.addAll(placeAdvanced.getOrDefault(pool.stage(), Set.of()));
        }
        List<ResultEntry> finishers = new ArrayList<>();
        for (HeatResults h : sr.heats()) {
            for (ResultEntry r : h.results()) {
                if (r.hasValidTime() && !excluded.contains(r.athleteId())) {
                    finishers.add(r);
                }
            }
        }
        finishers.sort(Comparator.comparingLong(ResultEntry::timeMs));
        return finishers;
    }

    /** Все спортсмены, занявшие место {@code place} в заездах этапа, отсортированные по времени (для «X из Y-ых»). */
    private List<ResultEntry> placePool(StageHistory history, String stage, int place) {
        StageResults sr = history.get(stage);
        if (sr == null) {
            return List.of();
        }
        List<ResultEntry> pool = new ArrayList<>();
        for (HeatResults h : sr.heats()) {
            h.atPlace(place).ifPresent(pool::add);
        }
        pool.sort(Comparator.comparingLong(ResultEntry::timeMs));
        return pool;
    }

    /** Множество спортсменов, прошедших дальше по месту, по этапам-источникам (для exclude_advanced). */
    private Map<String, Set<Long>> collectAdvancedByPlace(Plan plan, String activeVariant, StageHistory history) {
        Map<String, Set<Long>> result = new LinkedHashMap<>();
        if (plan.stages() == null) {
            return result;
        }
        for (StageSpec stage : plan.stages()) {
            for (Assignment a : stage.assignmentsFor(activeVariant)) {
                if (a.source() instanceof PlaceSource ps) {
                    resolvePlace(history, ps.from().stage(), ps.from().heat(), ps.place())
                            .ifPresent(entry -> result
                                    .computeIfAbsent(ps.from().stage(), k -> new HashSet<>())
                                    .add(entry.athleteId()));
                }
            }
        }
        return result;
    }

    // --- группировка и сборка результата ---------------------------------------------------

    /** Порядок групп best_time: для финала — порядок финалов (только включённые); для полуфинала — одна группа. */
    private List<String> bestTimeGroups(StageSpec stage, boolean toFinal, Set<String> enabledFinals) {
        if (!toFinal) {
            return List.of("semifinal");
        }
        List<String> groups = new ArrayList<>();
        for (String letter : stage.finalResolutionOrder()) {
            if (enabledFinals.contains(letter)) {
                groups.add(letter);
            }
        }
        return groups;
    }

    private boolean inGroup(Target target, boolean toFinal, String group) {
        return toFinal ? (target.isFinal() && group.equals(target.finalLetter())) : !target.isFinal();
    }

    private boolean isDisabled(Target target, boolean toFinal, Set<String> enabledFinals) {
        return toFinal && target.isFinal() && !enabledFinals.contains(target.finalLetter());
    }

    private void assign(Map<String, List<LaneAssignment>> byHeat, Set<Long> assigned,
                        Target target, long athleteId) {
        byHeat.computeIfAbsent(heatKey(target), k -> new ArrayList<>())
                .add(new LaneAssignment(target.lane(), athleteId));
        assigned.add(athleteId);
    }

    private StageDraw buildStageDraw(String toStage, StageSpec stage, Set<String> enabledFinals,
                                     Map<String, List<LaneAssignment>> byHeat) {
        List<HeatDraw> heats = new ArrayList<>();
        if ("final".equals(toStage)) {
            List<String> finals = stage.finals() != null ? stage.finals() : List.of("A");
            for (String letter : finals) {
                if (!enabledFinals.contains(letter)) {
                    continue;
                }
                List<LaneAssignment> lanes = byHeat.get("final " + letter);
                if (lanes != null && !lanes.isEmpty()) {
                    heats.add(HeatDraw.finalHeat(letter, lanes));
                }
            }
        } else {
            int count = stage.heatsCount() != null ? stage.heatsCount() : byHeat.size();
            for (int i = 1; i <= count; i++) {
                List<LaneAssignment> lanes = byHeat.get("semifinal " + i);
                if (lanes != null && !lanes.isEmpty()) {
                    heats.add(HeatDraw.semifinal(i, lanes));
                }
            }
        }
        return new StageDraw(toStage, heats);
    }

    private static String heatKey(Target target) {
        return target.isFinal() ? "final " + target.finalLetter() : "semifinal " + target.heat();
    }
}
