package ru.rowing.app.web.view;

import ru.rowing.app.domain.BoatClass;
import ru.rowing.app.domain.CategoryStatus;
import ru.rowing.app.domain.CompetitionStatus;
import ru.rowing.app.domain.Heat;
import ru.rowing.app.domain.StageType;
import ru.rowing.seeding.runtime.ResultStatus;

/** Русскоязычные подписи статусов и этапов для публичных и судейских страниц. */
public final class Labels {

    private Labels() {
    }

    public static String categoryStatus(CategoryStatus status) {
        return switch (status) {
            case DRAFT -> "Черновик";
            case REGISTRATION_OPEN -> "Регистрация открыта";
            case PROTOCOL_FORMED -> "Протокол сформирован";
            case PRELIMS_RUNNING -> "Идут предварительные";
            case SEMIS_READY -> "Готов к полуфиналам";
            case SEMIS_RUNNING -> "Идут полуфиналы";
            case FINALS_READY -> "Готов к финалам";
            case FINALS_RUNNING -> "Идут финалы";
            case FINISHED -> "Завершено";
        };
    }

    public static String competitionStatus(CompetitionStatus status) {
        return switch (status) {
            case SCHEDULED -> "Назначено";
            case LIVE -> "Идёт сейчас";
            case FINISHED -> "Завершено";
        };
    }

    public static String resultStatus(ResultStatus status) {
        return switch (status) {
            case NOT_STARTED -> "—";
            case OK -> "";
            case DNF -> "DNF";
            case DNS -> "DNS";
            case DSQ -> "DSQ";
        };
    }

    /** Класс лодки в нотации протокола: K1 → «К-1», C2 → «С-2». */
    public static String boatClass(BoatClass bc) {
        return switch (bc) {
            case K1 -> "К-1";
            case K2 -> "К-2";
            case C1 -> "С-1";
            case C2 -> "С-2";
        };
    }

    /** Подпись этапа заезда: «Предв. заезд 1», «Полуфинал 2», «Финал A». */
    public static String stage(Heat heat) {
        StageType type = heat.getStage().getType();
        return switch (type) {
            case PRELIM -> "Предв. заезд " + heat.getIndexInStage();
            case SEMIFINAL -> "Полуфинал " + heat.getIndexInStage();
            case FINAL -> "Финал " + (heat.getFinalLetter() != null ? heat.getFinalLetter() : "A");
        };
    }
}
