package ru.rowing.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Заявка (раздел 5.6 ТЗ): кого тренер подал в категорию до закрытия регистрации.
 * Один спортсмен — одна заявка в категорию (раздел 9).
 */
@Entity
@Table(name = "entry", uniqueConstraints = @UniqueConstraint(columnNames = {"category_id", "athlete_id"}))
public class Entry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @ManyToOne
    @JoinColumn(name = "submitted_by_coach_id")
    private Coach submittedByCoach;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Athlete getAthlete() {
        return athlete;
    }

    public void setAthlete(Athlete athlete) {
        this.athlete = athlete;
    }

    public Coach getSubmittedByCoach() {
        return submittedByCoach;
    }

    public void setSubmittedByCoach(Coach submittedByCoach) {
        this.submittedByCoach = submittedByCoach;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
