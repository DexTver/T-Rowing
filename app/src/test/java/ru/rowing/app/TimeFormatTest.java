package ru.rowing.app;

import org.junit.jupiter.api.Test;
import ru.rowing.app.util.TimeFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TimeFormatTest {

    @Test
    void formatsSubMinuteAndWithMinutes() {
        assertEquals("41.23", TimeFormat.format(41_230L));
        assertEquals("1:05.43", TimeFormat.format(65_430L));
        assertEquals("", TimeFormat.format(null));
    }

    @Test
    void parsesBothForms() {
        assertEquals(41_230L, TimeFormat.parse("41.23"));
        assertEquals(65_430L, TimeFormat.parse("1:05.43"));
        assertEquals(65_430L, TimeFormat.parse("1:05,43"));
        assertNull(TimeFormat.parse("чепуха"));
    }

    @Test
    void roundTrip() {
        assertEquals("1:05.43", TimeFormat.format(TimeFormat.parse("1:05.43")));
    }
}
