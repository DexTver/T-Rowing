package ru.rowing.app.web.view;

import java.util.List;

/**
 * Модели представления для публичной части (раздел 7.1 ТЗ). Поля публичные и финальные,
 * чтобы Thymeleaf/SpEL обращался к ним напрямую и без зависимости от ленивых связей JPA
 * (open-in-view отключён — данные подготовлены в сервисе внутри транзакции).
 */
public final class PublicViews {

    private PublicViews() {
    }

    /** Строка списка соревнований на главной. */
    public static final class CompetitionSummary {
        public final long id;
        public final String name;
        public final String statusLabel;
        public final boolean live;
        public final String startsAt;

        public CompetitionSummary(long id, String name, String statusLabel, boolean live, String startsAt) {
            this.id = id;
            this.name = name;
            this.statusLabel = statusLabel;
            this.live = live;
            this.startsAt = startsAt;
        }
    }

    /** Строка результата в заезде. */
    public static final class ResultRow {
        public final Integer place;
        public final int lane;
        public final String athleteName;
        public final Integer birthYear;
        public final String region;
        public final String sportSchool;
        public final String coachName;
        public final String time;
        public final String statusLabel;

        public ResultRow(Integer place, int lane, String athleteName, Integer birthYear, String region,
                         String sportSchool, String coachName, String time, String statusLabel) {
            this.place = place;
            this.lane = lane;
            this.athleteName = athleteName;
            this.birthYear = birthYear;
            this.region = region;
            this.sportSchool = sportSchool;
            this.coachName = coachName;
            this.time = time;
            this.statusLabel = statusLabel;
        }
    }

    /**
     * Карточка заезда на странице соревнования (вид стартового протокола): заголовок + дорожки.
     * Раскрывается на месте, без перехода на отдельную страницу (раздел 7.1.3).
     */
    public static final class HeatCard {
        public final long id;
        public final int number;
        public final String time;
        public final String boatClass;
        public final int distance;
        public final String categoryName;
        public final String stageLabel;
        public final String advancement;
        public final boolean run;
        public final List<ResultRow> lanes;

        public HeatCard(long id, int number, String time, String boatClass, int distance, String categoryName,
                        String stageLabel, String advancement, boolean run, List<ResultRow> lanes) {
            this.id = id;
            this.number = number;
            this.time = time;
            this.boatClass = boatClass;
            this.distance = distance;
            this.categoryName = categoryName;
            this.stageLabel = stageLabel;
            this.advancement = advancement;
            this.run = run;
            this.lanes = lanes;
        }
    }

    /** День соревнования — отдельная таблица заездов (заезды отсортированы по номеру = по времени). */
    public static final class DayView {
        public final String title;
        public final List<HeatCard> heats;

        public DayView(String title, List<HeatCard> heats) {
            this.title = title;
            this.heats = heats;
        }
    }

    /** Страница соревнования: заезды сгруппированы по дням, внутри дня — по номеру заезда. */
    public static final class CompetitionView {
        public final long id;
        public final String name;
        public final String description;
        public final String statusLabel;
        public final List<DayView> days;

        public CompetitionView(long id, String name, String description, String statusLabel, List<DayView> days) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.statusLabel = statusLabel;
            this.days = days;
        }
    }

    /** Раскрытый заезд на отдельной странице (для прямых ссылок из поиска). */
    public static final class HeatView {
        public final long id;
        public final int number;
        public final long competitionId;
        public final String competitionName;
        public final String categoryName;
        public final String stageLabel;
        public final boolean run;
        public final List<ResultRow> results;

        public HeatView(long id, int number, long competitionId, String competitionName,
                        String categoryName, String stageLabel, boolean run, List<ResultRow> results) {
            this.id = id;
            this.number = number;
            this.competitionId = competitionId;
            this.competitionName = competitionName;
            this.categoryName = categoryName;
            this.stageLabel = stageLabel;
            this.run = run;
            this.results = results;
        }
    }

    /** Строка результата на странице фильтров/поиска. */
    public static final class ResultSearchRow {
        public final long competitionId;
        public final String competitionName;
        public final String categoryName;
        public final long heatId;
        public final int heatNumber;
        public final String stageLabel;
        public final String athleteName;
        public final String region;
        public final String sportSchool;
        public final String coachName;
        public final String time;
        public final String statusLabel;

        public ResultSearchRow(long competitionId, String competitionName, String categoryName,
                               long heatId, int heatNumber, String stageLabel, String athleteName,
                               String region, String sportSchool, String coachName,
                               String time, String statusLabel) {
            this.competitionId = competitionId;
            this.competitionName = competitionName;
            this.categoryName = categoryName;
            this.heatId = heatId;
            this.heatNumber = heatNumber;
            this.stageLabel = stageLabel;
            this.athleteName = athleteName;
            this.region = region;
            this.sportSchool = sportSchool;
            this.coachName = coachName;
            this.time = time;
            this.statusLabel = statusLabel;
        }
    }
}
