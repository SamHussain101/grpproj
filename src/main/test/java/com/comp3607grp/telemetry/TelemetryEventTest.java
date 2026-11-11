package com.comp3607grp.telemetry;
import org.juit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class TelemetryEventTest {
    private final LocalDateTime t = LocalDateTime.of(2026,11,9,10,0);

    @Test void createsEventWithDefaultValues() {
        TelemetryEvent e = new TelemetryEvent("COMPILE", t);
        assertEquals("COMPILE", e.getEventType());
        assertEquals(t, e.getTimestamp());
        assertEquals("IN PROGRESS", e.getStatus());
        assertTrue(e.getErrorMessages().isEmpty());
    }

    @Test void rejectsInvalidEventTypeOrTimestamp() {
        assertThrows(IllegalArgumentException.class, () -> new TelemetryEvent(" ", t));
        assertThrows(IllegalArgumentException.class, () -> new TelemetryEvent("RUN", null));
    }

    @Test void recordsCompilationErrorAndLineNumber() {
        TelemetryEvent e = new TelemetryEvent("COMPILE", t);
        e.addError("Missing semicolon", 12);
        assertEquals("Missing semicolon", e.getErrorMessages().get(0));
        assertEquals(12, e.getLineNumbers().get(0));
        e.addError(" ", 15);
        assertEquals(1, e.getErrorMessages().size());
    }

    @Test void rejectsNegativeActivityAndCharacterCounts() {
        TelemetryEvent e = new TelemetryEvent("EDITOR_ACTIVITY", t);
        assertThrows(IllegalArgumentException.class, () -> e.setActivityDurationSeconds(-1));
        assertThrows(IllegalArgumentException.class, () -> e.setInsertedCharacters(-1));
        assertThrows(IllegalArgumentException.class, () -> e.setDeletedCharacters(-1));
        assertThrows(IllegalArgumentException.class, () -> e.setIntervalSeconds(-2));
    }

    @Test void returnsDefensiveCopyOfErrors() {
        TelemetryEvent e = new TelemetryEvent("COMPILE", t);
        e.addError("Error", 3);
        e.getErrorMessages().clear();
        assertEquals(1, e.getErrorMessages().size());
    }

}
