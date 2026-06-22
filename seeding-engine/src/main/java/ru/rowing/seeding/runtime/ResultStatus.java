package ru.rowing.seeding.runtime;

/** Статус результата спортсмена в заезде (раздел 5.9 ТЗ). */
public enum ResultStatus {
    NOT_STARTED,
    OK,
    DNF,
    DNS,
    DSQ;

    /** Финишировал ли спортсмен с валидным временем (попадает в ранжирование/посев). */
    public boolean isFinisher() {
        return this == OK;
    }
}
