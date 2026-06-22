package ru.rowing.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Заезд (раздел 5.8 ТЗ). {@code number} — сквозной номер заезда по всему соревнованию
 * (для печати/навигации), что соответствует требованию сквозной нумерации многодневного соревнования.
 */
@Entity
@Table(name = "heat")
public class Heat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    /** День соревнования, в который проходит заезд (предв. и финалы могут быть в разные дни). */
    @ManyToOne
    @JoinColumn(name = "competition_day_id")
    private CompetitionDay competitionDay;

    /** Сквозной номер заезда в соревновании. */
    @Column(name = "number", nullable = false)
    private int number;

    /** Индекс заезда внутри этапа (например, «полуфинал 2»). */
    @Column(name = "index_in_stage", nullable = false)
    private int indexInStage;

    /** Буква финала A/B/C; null для предв./полуфиналов. */
    @Column(name = "final_letter", length = 1)
    private String finalLetter;

    @Column(name = "scheduled_start")
    private Instant scheduledStart;

    /** Массовый старт (раздел 8.10, дистанция 5000 м): ограничение в 9 дорожек не действует. */
    @Column(name = "mass_start", nullable = false)
    private boolean massStart = false;

    /** Правило прохода («1-6 л в п/ф» и т.п.) — свободный текст из протокола (раздел 7.1.2). */
    @Column(name = "advancement", length = 160)
    private String advancement;

    public Long getId() {
        return id;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public CompetitionDay getCompetitionDay() {
        return competitionDay;
    }

    public void setCompetitionDay(CompetitionDay competitionDay) {
        this.competitionDay = competitionDay;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getIndexInStage() {
        return indexInStage;
    }

    public void setIndexInStage(int indexInStage) {
        this.indexInStage = indexInStage;
    }

    public String getFinalLetter() {
        return finalLetter;
    }

    public void setFinalLetter(String finalLetter) {
        this.finalLetter = finalLetter;
    }

    public Instant getScheduledStart() {
        return scheduledStart;
    }

    public void setScheduledStart(Instant scheduledStart) {
        this.scheduledStart = scheduledStart;
    }

    public boolean isMassStart() {
        return massStart;
    }

    public void setMassStart(boolean massStart) {
        this.massStart = massStart;
    }

    public String getAdvancement() {
        return advancement;
    }

    public void setAdvancement(String advancement) {
        this.advancement = advancement;
    }
}
