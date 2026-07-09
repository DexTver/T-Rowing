package ru.rowing.app.web.view;

import java.util.List;

/** Модели представления для судейских страниц (раздел 7.3 ТЗ). Поля публичные — для прямого доступа из Thymeleaf. */
public final class JudgeViews {

    private JudgeViews() {
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

    public static final class DayRow {
        public final long id;
        public final int ordinal;
        public final String date;
        public final String time;

        public DayRow(long id, int ordinal, String date, String time) {
            this.id = id;
            this.ordinal = ordinal;
            this.date = date;
            this.time = time;
        }
    }

    public static final class CatRow {
        public final long id;
        public final String name;
        public final String dayLabel;
        public final int distance;
        public final String statusLabel;

        public CatRow(long id, String name, String dayLabel, int distance, String statusLabel) {
            this.id = id;
            this.name = name;
            this.dayLabel = dayLabel;
            this.distance = distance;
            this.statusLabel = statusLabel;
        }
    }

    public static final class ManageView {
        public final long id;
        public final String name;
        public final String description;
        public final String statusLabel;
        public final List<DayRow> days;
        public final List<CatRow> categories;

        public ManageView(long id, String name, String description, String statusLabel,
                          List<DayRow> days, List<CatRow> categories) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.statusLabel = statusLabel;
            this.days = days;
            this.categories = categories;
        }
    }

    public static final class LaneRow {
        public final int lane;
        public final String athleteName;
        public final String value;

        public LaneRow(int lane, String athleteName, String value) {
            this.lane = lane;
            this.athleteName = athleteName;
            this.value = value;
        }
    }

    public static final class HeatBlock {
        public final long id;
        public final int number;
        public final String label;
        public final boolean run;
        public final List<LaneRow> lanes;

        public HeatBlock(long id, int number, String label, boolean run, List<LaneRow> lanes) {
            this.id = id;
            this.number = number;
            this.label = label;
            this.run = run;
            this.lanes = lanes;
        }
    }

    public static final class StageBlock {
        public final String label;
        public final List<HeatBlock> heats;

        public StageBlock(String label, List<HeatBlock> heats) {
            this.label = label;
            this.heats = heats;
        }
    }

    public static final class EntryRow {
        public final String athleteName;
        public final int birthYear;
        public final String region;
        public final String school;

        public EntryRow(String athleteName, int birthYear, String region, String school) {
            this.athleteName = athleteName;
            this.birthYear = birthYear;
            this.region = region;
            this.school = school;
        }
    }

    public static final class CategoryView {
        public final long id;
        public final long competitionId;
        public final String competitionName;
        public final String name;
        public final String statusLabel;
        public final String plan;
        public final List<String> planChoices;
        public final String variant;
        public final List<String> variantOptions;
        public final boolean massStart;
        public final List<EntryRow> entries;
        public final List<StageBlock> stages;
        public final boolean canOpen;
        public final boolean canAddParticipant;
        public final boolean canClose;
        public final boolean canFormNext;
        public final boolean needsVariant;
        public final String nextActionLabel;

        public CategoryView(long id, long competitionId, String competitionName, String name, String statusLabel,
                            String plan, List<String> planChoices, String variant, List<String> variantOptions,
                            boolean massStart, List<EntryRow> entries, List<StageBlock> stages, boolean canOpen,
                            boolean canAddParticipant, boolean canClose, boolean canFormNext,
                            boolean needsVariant, String nextActionLabel) {
            this.id = id;
            this.competitionId = competitionId;
            this.competitionName = competitionName;
            this.name = name;
            this.statusLabel = statusLabel;
            this.plan = plan;
            this.planChoices = planChoices;
            this.variant = variant;
            this.variantOptions = variantOptions;
            this.massStart = massStart;
            this.entries = entries;
            this.stages = stages;
            this.canOpen = canOpen;
            this.canAddParticipant = canAddParticipant;
            this.canClose = canClose;
            this.canFormNext = canFormNext;
            this.needsVariant = needsVariant;
            this.nextActionLabel = nextActionLabel;
        }
    }

    public static final class HeatEntryView {
        public final long id;
        public final int number;
        public final String label;
        public final long categoryId;
        public final String categoryName;
        public final List<LaneRow> lanes;

        public HeatEntryView(long id, int number, String label, long categoryId,
                             String categoryName, List<LaneRow> lanes) {
            this.id = id;
            this.number = number;
            this.label = label;
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.lanes = lanes;
        }
    }
}
