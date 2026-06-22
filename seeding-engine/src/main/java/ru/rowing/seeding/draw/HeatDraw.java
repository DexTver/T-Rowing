package ru.rowing.seeding.draw;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;

/**
 * Раскладка одного заезда: дорожки с назначенными спортсменами.
 * Для полуфинала задан {@code indexInStage}, для финала — {@code finalLetter}.
 */
public record HeatDraw(Integer indexInStage, String finalLetter, List<LaneAssignment> lanes) {

    public static HeatDraw semifinal(int indexInStage, List<LaneAssignment> lanes) {
        return new HeatDraw(indexInStage, null, sorted(lanes));
    }

    public static HeatDraw finalHeat(String finalLetter, List<LaneAssignment> lanes) {
        return new HeatDraw(null, finalLetter, sorted(lanes));
    }

    /** Спортсмен на указанной дорожке, если есть. */
    public OptionalInt laneIndexOf(long athleteId) {
        for (int i = 0; i < lanes.size(); i++) {
            if (lanes.get(i).athleteId() == athleteId) {
                return OptionalInt.of(lanes.get(i).lane());
            }
        }
        return OptionalInt.empty();
    }

    private static List<LaneAssignment> sorted(List<LaneAssignment> lanes) {
        List<LaneAssignment> copy = new ArrayList<>(lanes);
        copy.sort(Comparator.comparingInt(LaneAssignment::lane));
        return List.copyOf(copy);
    }
}
