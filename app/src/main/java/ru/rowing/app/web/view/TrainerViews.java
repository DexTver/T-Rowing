package ru.rowing.app.web.view;

import java.util.List;

/** Модели представления для тренера (раздел 7.2 ТЗ). Поля публичные — для прямого доступа из Thymeleaf. */
public final class TrainerViews {

    private TrainerViews() {
    }

    public static final class CompetitionRow {
        public final long id;
        public final String name;
        public final boolean hasOpenRegistration;

        public CompetitionRow(long id, String name, boolean hasOpenRegistration) {
            this.id = id;
            this.name = name;
            this.hasOpenRegistration = hasOpenRegistration;
        }
    }

    /** Открытая для регистрации категория (для выпадающего списка с поиском). */
    public static final class OpenCategory {
        public final long id;
        public final String name;
        public final Integer birthFrom;
        public final Integer birthTo;

        public OpenCategory(long id, String name, Integer birthFrom, Integer birthTo) {
            this.id = id;
            this.name = name;
            this.birthFrom = birthFrom;
            this.birthTo = birthTo;
        }
    }

    /** Спортсмен тренера (для выпадающего списка с поиском по подстроке). */
    public static final class MyAthlete {
        public final long id;
        public final String name;
        public final int birthYear;

        public MyAthlete(long id, String name, int birthYear) {
            this.id = id;
            this.name = name;
            this.birthYear = birthYear;
        }
    }

    public static final class EntryRow {
        public final long id;
        public final String athleteName;
        public final int birthYear;
        public final String categoryName;
        public final boolean ageWarning;
        public final boolean canDelete;

        public EntryRow(long id, String athleteName, int birthYear, String categoryName,
                        boolean ageWarning, boolean canDelete) {
            this.id = id;
            this.athleteName = athleteName;
            this.birthYear = birthYear;
            this.categoryName = categoryName;
            this.ageWarning = ageWarning;
            this.canDelete = canDelete;
        }
    }

    public static final class CompetitionView {
        public final long id;
        public final String name;
        public final boolean anyOpen;
        public final List<OpenCategory> openCategories;
        public final List<MyAthlete> myAthletes;
        public final List<EntryRow> myEntries;

        public CompetitionView(long id, String name, boolean anyOpen, List<OpenCategory> openCategories,
                               List<MyAthlete> myAthletes, List<EntryRow> myEntries) {
            this.id = id;
            this.name = name;
            this.anyOpen = anyOpen;
            this.openCategories = openCategories;
            this.myAthletes = myAthletes;
            this.myEntries = myEntries;
        }
    }
}
