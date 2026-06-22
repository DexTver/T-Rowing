package ru.rowing.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * День многодневного соревнования (новое требование 2026-06-19).
 * К дню привязываются категории (например, 500 м — в день 1; 200 и 1000 м — в день 2),
 * у каждого дня своё время начала. Нумерация заездов остаётся сквозной по всему соревнованию.
 */
@Entity
@Table(name = "competition_day")
public class CompetitionDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    /** Порядковый номер дня в соревновании (1-based). */
    @Column(nullable = false)
    private int ordinal;

    @Column(name = "day_date", nullable = false)
    private LocalDate dayDate;

    /** Время начала заездов в этот день. */
    @Column(name = "start_time")
    private LocalTime startTime;

    public Long getId() {
        return id;
    }

    public Competition getCompetition() {
        return competition;
    }

    public void setCompetition(Competition competition) {
        this.competition = competition;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    public LocalDate getDayDate() {
        return dayDate;
    }

    public void setDayDate(LocalDate dayDate) {
        this.dayDate = dayDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }
}
