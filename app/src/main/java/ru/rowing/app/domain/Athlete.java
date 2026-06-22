package ru.rowing.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Спортсмен (раздел 5.2 ТЗ). Разряд и субъект — свободный текст. */
@Entity
@Table(name = "athlete")
public class Athlete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "birth_year", nullable = false)
    private int birthYear;

    @Column(name = "rank")
    private String rank;

    @Column(name = "region")
    private String region;

    @Column(name = "sport_school")
    private String sportSchool;

    @ManyToOne
    @JoinColumn(name = "coach_id")
    private Coach coach;

    /** Внешний номер из базы спортсменов («Номер»), если спортсмен импортирован. */
    @Column(name = "ext_number")
    private Long extNumber;

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(int birthYear) {
        this.birthYear = birthYear;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getSportSchool() {
        return sportSchool;
    }

    public void setSportSchool(String sportSchool) {
        this.sportSchool = sportSchool;
    }

    public Coach getCoach() {
        return coach;
    }

    public void setCoach(Coach coach) {
        this.coach = coach;
    }

    public Long getExtNumber() {
        return extNumber;
    }

    public void setExtNumber(Long extNumber) {
        this.extNumber = extNumber;
    }
}
