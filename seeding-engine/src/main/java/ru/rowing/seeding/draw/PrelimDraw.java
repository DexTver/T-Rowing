package ru.rowing.seeding.draw;

import java.util.List;

/**
 * Результат жеребьёвки предварительных заездов.
 *
 * @param seed  зерно ГПСЧ (сохраняется в {@code category.draw_seed} для воспроизводимости)
 * @param heats предварительные заезды с назначенными дорожками
 */
public record PrelimDraw(long seed, List<HeatDraw> heats) {
}
