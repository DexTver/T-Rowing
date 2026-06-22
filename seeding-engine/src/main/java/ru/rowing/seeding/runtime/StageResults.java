package ru.rowing.seeding.runtime;

import java.util.List;
import java.util.Optional;

/**
 * Результаты завершённого этапа.
 *
 * @param stage имя этапа ("prelim" / "semifinal" / "final"), как в файле сетки
 * @param heats заезды этапа
 */
public record StageResults(String stage, List<HeatResults> heats) {

    /** Заезд по его порядковому номеру в этапе (1-based). */
    public Optional<HeatResults> heat(int indexInStage) {
        return heats.stream().filter(h -> h.indexInStage() == indexInStage).findFirst();
    }
}
