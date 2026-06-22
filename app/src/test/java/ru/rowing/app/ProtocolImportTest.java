package ru.rowing.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.rowing.app.domain.Athlete;
import ru.rowing.app.domain.Category;
import ru.rowing.app.domain.StageType;
import ru.rowing.app.repo.AthleteRepository;
import ru.rowing.app.repo.CategoryRepository;
import ru.rowing.app.repo.CompetitionRepository;
import ru.rowing.app.repo.StageRepository;
import ru.rowing.app.service.protocol.ProtocolImportService;
import ru.rowing.app.service.protocol.ProtocolImportService.ImportResult;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    @Autowired AthleteRepository athletes;

    @Test
    void importsProtocolWithBaseSheet() throws Exception {
        ImportResult r;
        try (InputStream in = getClass().getResourceAsStream("/protocol/sample-protocol.xlsx")) {
            assertNotNull(in, "sample-protocol.xlsx должен быть в тест-ресурсах");
            r = importer.importFrom(in, "Тест");
        }

        // База спортсменов (12 строк) загружена в БД с внешними номерами
        assertEquals(12, r.baseAthletes());
        assertEquals(2, r.days(), "две программы по дням");
        assertTrue(r.heats() >= 5, "заездов: " + r.heats());

        // Имена разрешены по номеру из базы — никаких формул VLOOKUP
        assertTrue(athletes.findAll().stream().noneMatch(a ->
                a.getFullName().contains("VLOOKUP") || a.getFullName().startsWith("=")));
        // Спортсмен из базы получил человекочитаемое имя и внешний номер
        Athlete fromBase = athletes.findAll().stream()
                .filter(a -> a.getExtNumber() != null).findFirst().orElseThrow();
        assertTrue(fromBase.getFullName().matches("[А-Яа-яЁё\\- ]+"), fromBase.getFullName());

        // Строка протокола без номера импортируется по тексту (запасной путь)
        assertTrue(athletes.findAll().stream().anyMatch(a -> a.getFullName().startsWith("Внебазов")));

        // Все три типа этапов присутствуют
        var comp = competitions.findById(r.competitionId()).orElseThrow();
        boolean prelim = false, semi = false, fin = false;
        for (Category c : categories.findByCompetitionId(comp.getId())) {
            for (var s : stages.findByCategoryIdOrderByOrdinal(c.getId())) {
                prelim |= s.getType() == StageType.PRELIM;
                semi |= s.getType() == StageType.SEMIFINAL;
                fin |= s.getType() == StageType.FINAL;
            }
        }
        assertTrue(prelim && semi && fin, "должны быть предв., п/ф и финал");
    }
}
