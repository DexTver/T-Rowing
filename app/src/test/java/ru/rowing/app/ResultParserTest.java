package ru.rowing.app;

import org.junit.jupiter.api.Test;
import ru.rowing.app.service.ResultParser;
import ru.rowing.seeding.runtime.ResultStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultParserTest {

    @Test
    void parsesTimesAndTokens() {
        ResultParser.Parsed p = ResultParser.parse("""
                1 41.23
                2 1:05.40
                3 DNF
                4 dsq
                """);
        assertFalse(p.hasErrors(), () -> p.errors().toString());
        assertEquals(4, p.rows().size());
        assertEquals(ResultStatus.OK, p.rows().get(0).status());
        assertEquals(41_230L, p.rows().get(0).timeMs());
        assertEquals(65_400L, p.rows().get(1).timeMs());
        assertEquals(ResultStatus.DNF, p.rows().get(2).status());
        assertEquals(ResultStatus.DSQ, p.rows().get(3).status());
    }

    @Test
    void reportsBadTimeAndDuplicateLane() {
        ResultParser.Parsed p = ResultParser.parse("""
                1 чепуха
                2 41.00
                2 42.00
                """);
        assertEquals(1, p.rows().size());
        assertTrue(p.errors().stream().anyMatch(e -> e.contains("не распознано время")));
        assertTrue(p.errors().stream().anyMatch(e -> e.contains("повторно")));
    }

    @Test
    void ignoresBlankLines() {
        ResultParser.Parsed p = ResultParser.parse("\n  \n5 40.00\n");
        assertEquals(1, p.rows().size());
        assertEquals(5, p.rows().get(0).lane());
    }
}
