package ru.rowing.seeding;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.rowing.seeding.model.Plan;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Загрузка и хранение планов из JSON-файлов сеток.
 * Источник файлов абстрагирован: класспас-ресурсы, каталог на диске или строки.
 * Веб-слой/админка могут наполнять репозиторий из БД.
 */
public final class PlanRepository {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final List<Plan> plans = new ArrayList<>();

    public static Plan parse(String json) {
        try {
            return MAPPER.readValue(json, Plan.class);
        } catch (IOException e) {
            throw new SeedingException("Не удалось разобрать файл сетки: " + e.getMessage(), e);
        }
    }

    public static Plan parse(InputStream in) {
        try {
            return MAPPER.readValue(in, Plan.class);
        } catch (IOException e) {
            throw new SeedingException("Не удалось разобрать файл сетки: " + e.getMessage(), e);
        }
    }

    /** Загружает план из ресурса класспаса, например {@code "/seeding/plan_A.json"}. */
    public static Plan fromClasspath(String resource) {
        try (InputStream in = PlanRepository.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new SeedingException("Ресурс сетки не найден: " + resource);
            }
            return parse(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public PlanRepository add(Plan plan) {
        plans.add(plan);
        return this;
    }

    public PlanRepository loadDirectory(Path dir) {
        try (var stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path file : stream) {
                add(parse(Files.readString(file)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public List<Plan> all() {
        return List.copyOf(plans);
    }

    public Optional<Plan> byLetter(String letter) {
        return plans.stream().filter(p -> letter.equals(p.plan())).findFirst();
    }

    /** План, в чей диапазон участников попадает N (раздел 8.1, приложение A). */
    public Optional<Plan> forParticipantCount(int n) {
        return plans.stream()
                .filter(p -> p.participants() != null && p.participants().contains(n))
                .findFirst();
    }
}
