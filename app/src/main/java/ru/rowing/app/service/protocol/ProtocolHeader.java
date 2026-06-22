package ru.rowing.app.service.protocol;

import ru.rowing.app.domain.BoatClass;
import ru.rowing.app.domain.Gender;
import ru.rowing.app.domain.StageType;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Разбор строки-заголовка заезда из стартового протокола, например:
 * {@code "1 з-д   10.30  К-1  500 м  юноши до 15 л 1 предв (1-6 л в п/ф )"}.
 * Чистая логика без POI — тестируется отдельно.
 */
public final class ProtocolHeader {

    public record Parsed(
            int fileNumber,
            String time,
            BoatClass boatClass,
            int distance,
            String categoryText,
            StageType stage,
            Integer stageIndex,
            String advancement
    ) {
        public Gender gender() {
            return ProtocolHeader.gender(categoryText);
        }

        public int age() {
            return ProtocolHeader.age(categoryText);
        }

        /** Ключ дисциплины для группировки заездов в категорию (класс + дистанция + пол + возраст). */
        public String disciplineKey() {
            return boatClass + "|" + distance + "|" + gender() + "|" + age();
        }
    }

    private static final Pattern HEADER_START = Pattern.compile("^\\s*(\\d+)\\s*з\\s*-?\\s*д");
    private static final Pattern TIME = Pattern.compile("(\\d{1,2}[.:]\\d{2})");
    private static final Pattern BOAT = Pattern.compile("([КKкkСCсc])\\s*-?\\s*([12])");
    private static final Pattern DIST = Pattern.compile("(\\d{2,4})\\s*м(?![\\p{L}])");
    private static final Pattern PRELIM_IDX = Pattern.compile("(\\d+)\\s*предв");
    private static final Pattern SEMI_IDX = Pattern.compile("(\\d+)\\s*п/ф");
    private static final Pattern ADV = Pattern.compile("\\(([^)]*)\\)");
    private static final Pattern AGE = Pattern.compile("до\\s*(\\d+)");

    private ProtocolHeader() {
    }

    /** {@code true}, если строка похожа на заголовок заезда. */
    public static boolean isHeader(String raw) {
        return raw != null && HEADER_START.matcher(raw.trim()).find();
    }

    public static Optional<Parsed> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String s = raw.trim();
        Matcher hm = HEADER_START.matcher(s);
        if (!hm.find()) {
            return Optional.empty();
        }
        int fileNumber = Integer.parseInt(hm.group(1));
        String rest = s.substring(hm.end());

        String time = null;
        Matcher tm = TIME.matcher(rest);
        if (tm.find()) {
            time = tm.group(1).replace('.', ':');
        }

        Matcher bm = BOAT.matcher(rest);
        if (!bm.find()) {
            return Optional.empty();
        }
        char letter = bm.group(1).charAt(0);
        boolean kayak = "КKкk".indexOf(letter) >= 0;
        int seats = Integer.parseInt(bm.group(2));

        int distance = -1;
        int distEnd = bm.end();
        Matcher dm = DIST.matcher(rest);
        while (dm.find()) {
            if (dm.start() >= bm.end()) {
                distance = Integer.parseInt(dm.group(1));
                distEnd = dm.end();
                break;
            }
        }
        if (distance < 0) {
            return Optional.empty();
        }

        String catStage = rest.substring(distEnd).trim();
        String advancement = null;
        Matcher am = ADV.matcher(catStage);
        if (am.find()) {
            advancement = am.group(1).trim();
            catStage = (catStage.substring(0, am.start()) + " " + catStage.substring(am.end())).trim();
        }

        StageType stage;
        Integer index = null;
        String catText;
        String lower = catStage.toLowerCase();
        if (lower.contains("финал")) {
            stage = StageType.FINAL;
            catText = catStage.replaceAll("(?iu)финал", "");
        } else if (lower.contains("п/ф")) {
            stage = StageType.SEMIFINAL;
            int cut = lower.indexOf("п/ф");
            Matcher sm = SEMI_IDX.matcher(catStage);
            if (sm.find()) {
                index = Integer.parseInt(sm.group(1));
                cut = sm.start();
            }
            catText = catStage.substring(0, cut);
        } else if (lower.contains("предв")) {
            stage = StageType.PRELIM;
            int cut = lower.indexOf("предв");
            Matcher pm = PRELIM_IDX.matcher(catStage);
            if (pm.find()) {
                index = Integer.parseInt(pm.group(1));
                cut = pm.start();
            }
            catText = catStage.substring(0, cut);
        } else {
            stage = StageType.FINAL;
            catText = catStage;
        }
        catText = catText.replaceAll("\\s+", " ").trim();

        BoatClass bc = kayak
                ? (seats == 1 ? BoatClass.K1 : BoatClass.K2)
                : (seats == 1 ? BoatClass.C1 : BoatClass.C2);
        return Optional.of(new Parsed(fileNumber, time, bc, distance, catText, stage, index, advancement));
    }

    static Gender gender(String catText) {
        String l = catText.toLowerCase();
        if (l.contains("девуш") || l.contains("женщ") || l.contains("юниорк")) {
            return Gender.FEMALE;
        }
        if (l.contains("юнош") || l.contains("юниор") || l.contains("муж") || l.contains("мальчик")) {
            return Gender.MALE;
        }
        return Gender.MIXED;
    }

    static int age(String catText) {
        Matcher m = AGE.matcher(catText.toLowerCase());
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }
}
