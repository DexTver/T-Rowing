package ru.rowing.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Категория — вид программы (раздел 5.5 ТЗ). Привязана к соревнованию и (опционально) к дню.
 * Финал A проводится всегда; B/C — по галочкам (раздел 8.9: {@code enabled_finals}).
 */
@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @ManyToOne
    @JoinColumn(name = "competition_day_id")
    private CompetitionDay competitionDay;

    @Column(nullable = false)
    private String name;

    @Column(name = "birth_year_from")
    private Integer birthYearFrom;

    @Column(name = "birth_year_to")
    private Integer birthYearTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "boat_class", nullable = false, length = 4)
    private BoatClass boatClass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Gender gender;

    @Column(name = "distance_m", nullable = false)
    private int distanceM;

    /** Галочка «проводить финал B» (раздел 7.3 / 8.9). */
    @Column(name = "final_b_enabled", nullable = false)
    private boolean finalBEnabled = false;

    /** Галочка «проводить финал C». */
    @Column(name = "final_c_enabled", nullable = false)
    private boolean finalCEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private CategoryStatus status = CategoryStatus.DRAFT;

    @Column(name = "registration_opens_at")
    private Instant registrationOpensAt;

    @Column(name = "registration_closes_at")
    private Instant registrationClosesAt;

    /** Буква плана A–Q или {@code A-alt} (вычисляется/выбирается при формировании протокола). */
    @Column(length = 16)
    private String plan;

    @Column(name = "active_variant", length = 8)
    private String activeVariant;

    /** Сохранённое зерно ГПСЧ жеребьёвки (раздел 5.5, для воспроизводимости и аудита). */
    @Column(name = "draw_seed")
    private Long drawSeed;

    /** Интервал между заездами по умолчанию, секунды (раздел 5.10). */
    @Column(name = "default_interval_sec", nullable = false)
    private int defaultIntervalSec = 180;

    /** Реально формируемые финалы (раздел 8.9): всегда A, плюс B/C по галочкам. */
    public Set<String> enabledFinals() {
        Set<String> finals = new LinkedHashSet<>();
        finals.add("A");
        if (finalBEnabled) {
            finals.add("B");
        }
        if (finalCEnabled) {
            finals.add("C");
        }
        return finals;
    }

    public boolean isMassStart() {
        return distanceM == 5000;
    }

    public Long getId() {
        return id;
    }

    public Competition getCompetition() {
        return competition;
    }

    public void setCompetition(Competition competition) {
        this.competition = competition;
    }

    public CompetitionDay getCompetitionDay() {
        return competitionDay;
    }

    public void setCompetitionDay(CompetitionDay competitionDay) {
        this.competitionDay = competitionDay;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getBirthYearFrom() {
        return birthYearFrom;
    }

    public void setBirthYearFrom(Integer birthYearFrom) {
        this.birthYearFrom = birthYearFrom;
    }

    public Integer getBirthYearTo() {
        return birthYearTo;
    }

    public void setBirthYearTo(Integer birthYearTo) {
        this.birthYearTo = birthYearTo;
    }

    public BoatClass getBoatClass() {
        return boatClass;
    }

    public void setBoatClass(BoatClass boatClass) {
        this.boatClass = boatClass;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public int getDistanceM() {
        return distanceM;
    }

    public void setDistanceM(int distanceM) {
        this.distanceM = distanceM;
    }

    public boolean isFinalBEnabled() {
        return finalBEnabled;
    }

    public void setFinalBEnabled(boolean finalBEnabled) {
        this.finalBEnabled = finalBEnabled;
    }

    public boolean isFinalCEnabled() {
        return finalCEnabled;
    }

    public void setFinalCEnabled(boolean finalCEnabled) {
        this.finalCEnabled = finalCEnabled;
    }

    public CategoryStatus getStatus() {
        return status;
    }

    public void setStatus(CategoryStatus status) {
        this.status = status;
    }

    public Instant getRegistrationOpensAt() {
        return registrationOpensAt;
    }

    public void setRegistrationOpensAt(Instant registrationOpensAt) {
        this.registrationOpensAt = registrationOpensAt;
    }

    public Instant getRegistrationClosesAt() {
        return registrationClosesAt;
    }

    public void setRegistrationClosesAt(Instant registrationClosesAt) {
        this.registrationClosesAt = registrationClosesAt;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getActiveVariant() {
        return activeVariant;
    }

    public void setActiveVariant(String activeVariant) {
        this.activeVariant = activeVariant;
    }

    public Long getDrawSeed() {
        return drawSeed;
    }

    public void setDrawSeed(Long drawSeed) {
        this.drawSeed = drawSeed;
    }

    public int getDefaultIntervalSec() {
        return defaultIntervalSec;
    }

    public void setDefaultIntervalSec(int defaultIntervalSec) {
        this.defaultIntervalSec = defaultIntervalSec;
    }
}
