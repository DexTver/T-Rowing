package ru.rowing.seeding;

import ru.rowing.seeding.runtime.HeatResults;
import ru.rowing.seeding.runtime.ResultEntry;
import ru.rowing.seeding.runtime.ResultStatus;
import ru.rowing.seeding.runtime.StageResults;

import java.util.ArrayList;
import java.util.List;

/** Утилиты построения синтетических результатов для тестов посева. */
final class TestResults {

    private TestResults() {
    }

    /**
     * Заезд из {@code count} финишёров, где спортсмен с местом p имеет id = {@code heat*100 + p}
     * и время = {@code heat*10000 + 1000*p} (меньше время — выше место).
     */
    static HeatResults prelimHeat(int heat, int count) {
        List<ResultEntry> rows = new ArrayList<>();
        for (int place = 1; place <= count; place++) {
            long id = heat * 100L + place;
            long time = heat * 10000L + 1000L * place;
            rows.add(new ResultEntry(id, place, time, ResultStatus.OK));
        }
        return new HeatResults(heat, rows);
    }

    /** Результат одного спортсмена с заданными id/время (место вычислит движок по времени). */
    static ResultEntry ok(long id, int lane, long timeMs) {
        return new ResultEntry(id, lane, timeMs, ResultStatus.OK);
    }

    static StageResults stage(String name, HeatResults... heats) {
        return new StageResults(name, List.of(heats));
    }
}
