package ru.rowing.app;

import org.junit.jupiter.api.Test;
import ru.rowing.app.domain.BoatClass;
import ru.rowing.app.domain.Gender;
import ru.rowing.app.domain.StageType;
import ru.rowing.app.service.protocol.ProtocolHeader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocolHeaderTest {

    @Test
    void parsesPrelimHeader() {
        var p = ProtocolHeader.parse("1 з-д        10.30  К-1  500 м  юноши до 15 л 1 предв (1-6 л в п/ф )").orElseThrow();
        assertEquals(1, p.fileNumber());
        assertEquals("10:30", p.time());
        assertEquals(BoatClass.K1, p.boatClass());
        assertEquals(500, p.distance());
        assertEquals(StageType.PRELIM, p.stage());
        assertEquals(1, p.stageIndex());
        assertEquals("юноши до 15 л", p.categoryText());
        assertEquals("1-6 л в п/ф", p.advancement());
        assertEquals(Gender.MALE, p.gender());
        assertEquals(15, p.age());
    }

    @Test
    void parsesSemifinalWithGluedIndex() {
        var p = ProtocolHeader.parse("23 з-д        12.15 К-1  500 м  муж    1п/ф  (1 -4 л в фин +луч по времени в фин )").orElseThrow();
        assertEquals(StageType.SEMIFINAL, p.stage());
        assertEquals(1, p.stageIndex());
        assertEquals("муж", p.categoryText());
        assertEquals(Gender.MALE, p.gender());
    }

    @Test
    void parsesFinalWithoutIndex() {
        var p = ProtocolHeader.parse("33 з-д        13.05  К-1  500 м  юниоры  до 15 лет  ФИНАЛ").orElseThrow();
        assertEquals(StageType.FINAL, p.stage());
        assertNull(p.stageIndex());
        assertEquals("юниоры до 15 лет", p.categoryText());
        assertNull(p.advancement());
        assertEquals(15, p.age());
    }

    @Test
    void parsesCanoeAnd2000mNoStageAsFinal() {
        var p = ProtocolHeader.parse("4 з-д        10.00  К-1  2000 м  девушки до 13 лет").orElseThrow();
        assertEquals(2000, p.distance());
        assertEquals(StageType.FINAL, p.stage());
        assertEquals(Gender.FEMALE, p.gender());
        assertEquals(13, p.age());

        var c = ProtocolHeader.parse("25 з-д        12.25 С-1  500 м  юноши до 15 лет  1 п/ф  (1 -3 л в фин )").orElseThrow();
        assertEquals(BoatClass.C1, c.boatClass());
        assertEquals(StageType.SEMIFINAL, c.stage());
    }

    @Test
    void rejectsNonHeaderRows() {
        assertFalse(ProtocolHeader.isHeader("1"));
        assertFalse(ProtocolHeader.isHeader("Иванов Иван"));
        assertTrue(ProtocolHeader.parse("просто текст").isEmpty());
    }
}
