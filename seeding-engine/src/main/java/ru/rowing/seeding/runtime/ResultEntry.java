package ru.rowing.seeding.runtime;

/**
 * Результат одного спортсмена в заезде.
 *
 * @param athleteId идентификатор спортсмена (непрозрачный для движка)
 * @param lane      дорожка в заезде
 * @param timeMs    время в миллисекундах; {@code null}, если нет валидного времени
 * @param status    статус результата
 */
public record ResultEntry(long athleteId, int lane, Long timeMs, ResultStatus status) {

    /** Есть ли валидное время для ранжирования (статус OK и время задано). */
    public boolean hasValidTime() {
        return status != null && status.isFinisher() && timeMs != null;
    }
}
