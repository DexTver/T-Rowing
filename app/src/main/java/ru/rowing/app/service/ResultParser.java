package ru.rowing.app.service;

import ru.rowing.app.util.TimeFormat;
import ru.rowing.seeding.runtime.ResultStatus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Быстрый ввод результатов судьёй (раздел 7.3, п.4): построчно {@code номер_дорожки␣время}.
 * Вместо времени допускаются токены {@code DNF}/{@code DNS}/{@code DSQ}. Парсер устойчив к ошибкам:
 * непонятные строки попадают в {@link Parsed#errors()}, остальные — в {@link Parsed#rows()}.
 */
public final class ResultParser {

    public record Row(int lane, ResultStatus status, Long timeMs) {
    }

    public record Parsed(List<Row> rows, List<String> errors) {
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }

    private ResultParser() {
    }

    public static Parsed parse(String text) {
        List<Row> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<Integer> seenLanes = new HashSet<>();
        if (text == null) {
            return new Parsed(rows, errors);
        }

        for (String raw : text.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\s+", 2);
            if (parts.length < 2) {
                errors.add("Строка «" + line + "»: ожидается «дорожка время».");
                continue;
            }
            int lane;
            try {
                lane = Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {
                errors.add("Строка «" + line + "»: дорожка не число.");
                continue;
            }
            if (!seenLanes.add(lane)) {
                errors.add("Дорожка " + lane + " указана повторно.");
                continue;
            }

            String value = parts[1].trim();
            ResultStatus token = statusToken(value);
            if (token != null) {
                rows.add(new Row(lane, token, null));
                continue;
            }
            Long ms = TimeFormat.parse(value);
            if (ms == null) {
                errors.add("Дорожка " + lane + ": не распознано время «" + value + "».");
                continue;
            }
            rows.add(new Row(lane, ResultStatus.OK, ms));
        }
        return new Parsed(rows, errors);
    }

    private static ResultStatus statusToken(String value) {
        return switch (value.toUpperCase()) {
            case "DNF" -> ResultStatus.DNF;
            case "DNS" -> ResultStatus.DNS;
            case "DSQ" -> ResultStatus.DSQ;
            default -> null;
        };
    }
}
