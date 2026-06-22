package ru.rowing.app.web.view;

import java.util.List;

/** Модели представления для админки (раздел 7.4 ТЗ). Поля публичные — для прямого доступа из Thymeleaf. */
public final class AdminViews {

    private AdminViews() {
    }

    public static final class UserRow {
        public final long id;
        public final String login;
        public final String role;
        public final String displayName;
        public final boolean active;

        public UserRow(long id, String login, String role, String displayName, boolean active) {
            this.id = id;
            this.login = login;
            this.role = role;
            this.displayName = displayName;
            this.active = active;
        }
    }

    public static final class CoachRow {
        public final long id;
        public final String fullName;
        public final String userLogin;

        public CoachRow(long id, String fullName, String userLogin) {
            this.id = id;
            this.fullName = fullName;
            this.userLogin = userLogin;
        }
    }

    public static final class AthleteRow {
        public final long id;
        public final String fullName;
        public final int birthYear;
        public final String rank;
        public final String region;
        public final String school;
        public final Long coachId;
        public final String coachName;

        public AthleteRow(long id, String fullName, int birthYear, String rank, String region,
                          String school, Long coachId, String coachName) {
            this.id = id;
            this.fullName = fullName;
            this.birthYear = birthYear;
            this.rank = rank;
            this.region = region;
            this.school = school;
            this.coachId = coachId;
            this.coachName = coachName;
        }
    }

    public static final class CompetitionRow {
        public final long id;
        public final String name;
        public final String statusLabel;

        public CompetitionRow(long id, String name, String statusLabel) {
            this.id = id;
            this.name = name;
            this.statusLabel = statusLabel;
        }
    }

    public static final class Option {
        public final long id;
        public final String label;

        public Option(long id, String label) {
            this.id = id;
            this.label = label;
        }
    }

    public static final class PlanRow {
        public final String letter;
        public final String source;
        public final boolean valid;
        public final String violations;
        public final Integer min;
        public final Integer max;

        public PlanRow(String letter, String source, boolean valid, String violations, Integer min, Integer max) {
            this.letter = letter;
            this.source = source;
            this.valid = valid;
            this.violations = violations;
            this.min = min;
            this.max = max;
        }
    }

    public static final class Overview {
        public final long users;
        public final long coaches;
        public final long athletes;
        public final long competitions;
        public final long categories;
        public final long entries;
        public final long heats;
        public final long results;

        public Overview(long users, long coaches, long athletes, long competitions,
                        long categories, long entries, long heats, long results) {
            this.users = users;
            this.coaches = coaches;
            this.athletes = athletes;
            this.competitions = competitions;
            this.categories = categories;
            this.entries = entries;
            this.heats = heats;
            this.results = results;
        }
    }

    public static final class PlansView {
        public final List<PlanRow> plans;
        public final String coverage;

        public PlansView(List<PlanRow> plans, String coverage) {
            this.plans = plans;
            this.coverage = coverage;
        }
    }
}
