package ru.rowing.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.rowing.app.domain.Category;
import ru.rowing.app.domain.Heat;
import ru.rowing.app.domain.Result;
import ru.rowing.app.domain.Stage;
import ru.rowing.app.domain.StageType;
import ru.rowing.app.repo.CategoryRepository;
import ru.rowing.app.repo.HeatRepository;
import ru.rowing.app.repo.ResultRepository;
import ru.rowing.app.repo.StageRepository;
import ru.rowing.app.service.JudgeService;
import ru.rowing.app.service.PlanCatalog;
import ru.rowing.app.service.protocol.ProtocolImportService;
import ru.rowing.seeding.SeedingException;
import ru.rowing.seeding.runtime.ResultStatus;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Полная цепочка для импортированного протокола: план определён по N, судья выбирает вариант,
 * движок доформировывает пустые полуфиналы из результатов предвар.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ImportedFormationTest {

    @Autowired ProtocolImportService importer;
    @Autowired JudgeService judge;
    @Autowired PlanCatalog catalog;
    @Autowired CategoryRepository categories;
    @Autowired StageRepository stages;
    @Autowired HeatRepository heats;
    @Autowired ResultRepository results;

    @Test
    void planDetectionVariantChoiceAndFormation() throws Exception {
        // каталог: N -> буква, и варианты плана
        assertEquals("A", catalog.planLetterForCount(12));
        assertTrue(catalog.planLetterForCount(5) == null, "N<10 -> без плана");
        assertEquals(List.of("1", "2"), catalog.variantOptions("A"));

        long compId;
        try (InputStream in = getClass().getResourceAsStream("/protocol/sample-protocol.xlsx")) {
            compId = importer.importFrom(in, "Тест").competitionId();
        }
        Category cat = categories.findByCompetitionId(compId).stream()
                .filter(c -> "A".equals(c.getPlan())).findFirst().orElseThrow();

        // без выбранного варианта формирование запрещено
        enterAllPrelimResults(cat);
        assertThrows(SeedingException.class, () -> judge.formNextStage(cat.getId()));

        // выбираем вариант и формируем полуфиналы
        judge.setVariant(cat.getId(), "1");
        judge.formNextStage(cat.getId());

        List<Stage> after = stages.findByCategoryIdOrderByOrdinal(cat.getId());
        Stage semi = after.stream().filter(s -> s.getType() == StageType.SEMIFINAL).findFirst()
                .orElseThrow(() -> new AssertionError("полуфинал не сформирован"));
        long lanes = heats.findByStageId(semi.getId()).stream()
                .mapToLong(h -> results.findByHeatId(h.getId()).size()).sum();
        assertTrue(lanes > 0, "в полуфинале должны появиться спортсмены");
    }

    private void enterAllPrelimResults(Category cat) {
        Stage prelim = stages.findByCategoryIdOrderByOrdinal(cat.getId()).stream()
                .filter(s -> s.getType() == StageType.PRELIM).findFirst().orElseThrow();
        long t = 60_000;
        for (Heat h : heats.findByStageId(prelim.getId())) {
            for (Result r : results.findByHeatId(h.getId())) {
                r.setStatus(ResultStatus.OK);
                r.setTimeMs(t += 1234);
                results.save(r);
            }
        }
    }
}
