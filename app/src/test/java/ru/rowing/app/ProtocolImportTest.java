package ru.rowing.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.rowing.app.domain.Category;
import ru.rowing.app.domain.StageType;
import ru.rowing.app.repo.CategoryRepository;
import ru.rowing.app.repo.CompetitionRepository;
import ru.rowing.app.repo.StageRepository;
import ru.rowing.app.service.protocol.ProtocolImportService;
import ru.rowing.app.service.protocol.ProtocolImportService.ImportResult;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Импорт стартового протокола из синтетического примера (структура «база» + протокол).
 * Реальные данные спортсменов в репозиторий не включаются (персональные данные).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProtocolImportTest {

    @Autowired ProtocolImportService importer;
    @Autowired CompetitionRepository competitions;
    @Autowired CategoryRepository categories;
    @Autowired StageRepository stages;

    @Test
    void importsProtocolWithBaseSheet() throws Exception {
        ImportResult r;
        try (InputStream in = getClass().getResourceAsStream("/protocol/sample-protocol.xlsx")) {
            assertNotNull(in, "sample-protocol.xlsx должен быть в тест-ресурсах");
            r = importer.importFrom(in, "Тест");
        }

        assertEquals(12, r.baseAthletes());
        assertEquals(2, r.days(), "две программы по дням");

        var comp = competitions.findById(r.competitionId()).orElseThrow();
        var cats = categories.findByCompetitionId(comp.getId());

        // Категория К-1 500 (2 предв. заезда, 12 участников) получает план A по числу участников.
        Category k1_500 = cats.stream().filter(c -> c.getName().startsWith("К-1 500")).findFirst().orElseThrow();
        assertEquals("A", k1_500.getPlan(), "N=12 -> план A");
        assertNull(k1_500.getActiveVariant(), "вариант выбирает судья");

        // Пустые заготовки (полуфиналы/финалы без участников) пропущены — остаётся только предв. этап.
        assertTrue(stages.findByCategoryIdOrderByOrdinal(k1_500.getId()).stream()
                .allMatch(s -> s.getType() == StageType.PRELIM), "у стартового протокола создан только предв. этап");

        // Категория С-1 200 (6 участников) — плана нет (N<10).
        Category c1_200 = cats.stream().filter(c -> c.getName().startsWith("С-1 200")).findFirst().orElseThrow();
        assertNull(c1_200.getPlan());
    }
}
