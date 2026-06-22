package ru.rowing.app.domain;

/** Состояние конечного автомата категории (раздел 6 ТЗ). */
public enum CategoryStatus {
    DRAFT,
    REGISTRATION_OPEN,
    PROTOCOL_FORMED,
    PRELIMS_RUNNING,
    SEMIS_READY,
    SEMIS_RUNNING,
    FINALS_READY,
    FINALS_RUNNING,
    FINISHED
}
