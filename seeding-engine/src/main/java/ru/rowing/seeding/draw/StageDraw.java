package ru.rowing.seeding.draw;

import java.util.List;
import java.util.Optional;

/**
 * Раскладка сформированного этапа: набор заездов.
 *
 * @param stage имя этапа ("prelim" / "semifinal" / "final")
 * @param heats заезды этапа
 */
public record StageDraw(String stage, List<HeatDraw> heats) {

    public Optional<HeatDraw> semifinal(int indexInStage) {
        return heats.stream()
                .filter(h -> h.indexInStage() != null && h.indexInStage() == indexInStage)
                .findFirst();
    }

    public Optional<HeatDraw> finalHeat(String letter) {
        return heats.stream()
                .filter(h -> letter.equals(h.finalLetter()))
                .findFirst();
    }
}
