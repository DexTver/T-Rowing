package ru.rowing.seeding.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Накопленные результаты завершённых этапов категории — вход для посева следующего этапа.
 * Ключ — имя этапа ("prelim" / "semifinal" / "final").
 */
public final class StageHistory {

    private final Map<String, StageResults> byStage = new LinkedHashMap<>();

    public StageHistory add(StageResults stage) {
        byStage.put(stage.stage(), stage);
        return this;
    }

    public StageResults get(String stage) {
        return byStage.get(stage);
    }

    public boolean has(String stage) {
        return byStage.containsKey(stage);
    }

    public static StageHistory of(StageResults... stages) {
        StageHistory h = new StageHistory();
        for (StageResults s : stages) {
            h.add(s);
        }
        return h;
    }
}
