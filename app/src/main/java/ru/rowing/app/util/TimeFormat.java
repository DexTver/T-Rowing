package ru.rowing.app.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Единая точка парсинга и форматирования времени прохождения (раздел 9 ТЗ).
 * Хранится как целое {@code time_ms}; ввод/вывод в формате {@code мм:сс.сс} или {@code сс.сс}.
 */
public final class TimeFormat {

    private static final Pattern PATTERN = Pattern.compile(
            "^\\s*(?:(\\d{1,2}):)?([0-5]?\\d)[.,](\\d{1,2})\\s*$");

    private TimeFormat() {
    }

    /** Форматирует миллисекунды в {@code м:сс.сс} (минуты опускаются, если их нет). Null → "". */
    public static String format(Long ms) {
        if (ms == null) {
            return "";
        }
        long total = ms;
        long minutes = total / 60_000;
        long seconds = (total % 60_000) / 1000;
        long hundredths = (total % 1000) / 10;
        if (minutes > 0) {
            return String.format("%d:%02d.%02d", minutes, seconds, hundredths);
        }
        return String.format("%d.%02d", seconds, hundredths);
    }

    /**
     * Парсит строку {@code мм:сс.сс} / {@code сс.сс} в миллисекунды.
     *
     * @return миллисекунды или {@code null}, если строка не распознана
     */
    public static Long parse(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = PATTERN.matcher(text);
        if (!m.matches()) {
            return null;
        }
        long minutes = m.group(1) != null ? Long.parseLong(m.group(1)) : 0;
        long seconds = Long.parseLong(m.group(2));
        String frac = m.group(3);
        long hundredths = frac.length() == 1 ? Long.parseLong(frac) * 10 : Long.parseLong(frac);
        return ((minutes * 60 + seconds) * 1000) + hundredths * 10;
    }
}
