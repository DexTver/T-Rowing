package ru.rowing.seeding;

import ru.rowing.seeding.model.Plan;
import ru.rowing.seeding.model.Prelims;

import java.util.ArrayList;
import java.util.List;

/**
 * Расчёт размеров предварительных заездов по числу участников N (раздел 8.2–8.3).
 * Поддерживает формулу {@code even_desc} (поровну, большие заезды — первыми)
 * и явную таблицу {@code sizes} по N.
 */
public final class PrelimSizing {

    private PrelimSizing() {
    }

    public static List<Integer> sizesFor(Plan plan, int n) {
        Prelims prelims = plan.prelims();
        if (prelims == null) {
            throw new SeedingException("План " + plan.plan() + " не описывает предварительные заезды");
        }

        if (prelims.sizes() != null && prelims.sizes().containsKey(Integer.toString(n))) {
            return List.copyOf(prelims.sizes().get(Integer.toString(n)));
        }

        String sizing = prelims.sizing();
        if ("even_desc".equals(sizing)) {
            return evenDesc(n, prelims.count());
        }

        if (sizing == null && prelims.sizes() != null) {
            throw new SeedingException("Нет явных размеров заездов для N=" + n + " в плане " + plan.plan());
        }
        throw new SeedingException("Неизвестная стратегия размеров заездов: " + sizing);
    }

    /** N разложить по {@code count} заездам поровну; остаток добавляется в первые заезды. */
    public static List<Integer> evenDesc(int n, int count) {
        if (count <= 0) {
            throw new SeedingException("Число предварительных заездов должно быть положительным");
        }
        int base = n / count;
        int rem = n % count;
        List<Integer> sizes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            sizes.add(base + (i < rem ? 1 : 0));
        }
        return sizes;
    }
}
