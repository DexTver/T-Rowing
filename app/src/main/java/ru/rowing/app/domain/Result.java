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
import ru.rowing.seeding.runtime.ResultStatus;

/**
 * Результат спортсмена в заезде (раздел 5.9 ТЗ). Статус переиспользуется из движка
 * ({@link ResultStatus}); время — целое {@code time_ms}, nullable. Дорожка 1–10 (10 — тай-брейк, 8.4).
 */
@Entity
@Table(name = "result")
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "heat_id", nullable = false)
    private Heat heat;

    @ManyToOne(optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @Column(nullable = false)
    private int lane;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ResultStatus status = ResultStatus.NOT_STARTED;

    @Column(name = "time_ms")
    private Long timeMs;

    public Long getId() {
        return id;
    }

    public Heat getHeat() {
        return heat;
    }

    public void setHeat(Heat heat) {
        this.heat = heat;
    }

    public Athlete getAthlete() {
        return athlete;
    }

    public void setAthlete(Athlete athlete) {
        this.athlete = athlete;
    }

    public int getLane() {
        return lane;
    }

    public void setLane(int lane) {
        this.lane = lane;
    }

    public ResultStatus getStatus() {
        return status;
    }

    public void setStatus(ResultStatus status) {
        this.status = status;
    }

    public Long getTimeMs() {
        return timeMs;
    }

    public void setTimeMs(Long timeMs) {
        this.timeMs = timeMs;
    }
}
