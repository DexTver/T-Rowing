package ru.rowing.seeding.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Результаты одного заезда внутри этапа.
 *
 * @param indexInStage порядковый номер заезда в этапе (1-based), как в посеве сетки
 * @param results      результаты спортсменов этого заезда
 */
public record HeatResults(int indexInStage, List<ResultEntry> results) {

    /** Финишировавшие участники, отсортированные по возрастанию времени (1-е место — первое). */
    public List<ResultEntry> rankedByTime() {
        List<ResultEntry> finishers = new ArrayList<>();
        for (ResultEntry r : results) {
            if (r.hasValidTime()) {
                finishers.add(r);
            }
        }
        finishers.sort(Comparator.comparingLong(ResultEntry::timeMs));
        return finishers;
    }

    /** Спортсмен, занявший место {@code place} (1-based) по времени; пусто, если столько финишёров нет. */
    public Optional<ResultEntry> atPlace(int place) {
        List<ResultEntry> ranked = rankedByTime();
        if (place < 1 || place > ranked.size()) {
            return Optional.empty();
        }
        return Optional.of(ranked.get(place - 1));
    }
}
