package com.comp3607grp.telemetry;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TelemetryEventTest {

    private final LocalDateTime t = LocalDateTime.of(2026, 11, 9, 10, 0);

    @Test void createsEventWithDefaultValues() {
        TelemetryEvent e = new TelemetryEvent("COMPILE", t);
        assertEquals("COMPILE", e.getEventType());
        assertEquals(t, e.getTimestamp());
        assertEquals("IN PROGRESS", e.getStatus());
        assertEquals(TelemetryEvent.DEFAULT_STATUS, e.getStatus());
        assertTrue(e.getErrorMessages().isEmpty());
        assertTrue(e.getLineNumbers().isEmpty());
        assertEquals(TelemetryEvent.NO_INTERVAL, e.getIntervalSeconds());
        assertEquals(0, e.getActivityDurationSeconds());
        assertEquals(0, e.getInsertedCharacters());
        assertEquals(0, e.getDeletedCharacters());
        assertNull(e.getErrorCode());
        assertNull(e.getExceptionMessage());
        assertNull(e.getConsoleSummary());
        assertNull(e.getProjectName());
        assertNull(e.getClassName());
        assertNull(e.getMethodName());
        assertNull(e.getObjectName());
    }

    @Test void rejectsInvalidEventTypeOrTimestamp() {
        assertThrows(IllegalArgumentException.class, () -> new TelemetryEvent(" ", t));
        assertThrows(IllegalArgumentException.class, () -> new TelemetryEvent(null, t));
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

    @Test void ignoresNullOrBlankErrorMessages() {
        TelemetryEvent e = new TelemetryEvent("COMPILE", t);
        e.addError(null, 1);
        e.addError("", 2);
        e.addError("   ", 3);
        assertTrue(e.getErrorMessages().isEmpty());
        assertTrue(e.getLineNumbers().isEmpty());
    }

    @Test void keepsErrorMessagesAndLineNumbersAligned() {
        TelemetryEvent e = new TelemetryEvent("COMPILE", t);
        e.addError("first", 3);
        e.addError(null, 99);
        e.addError("second", 8);
        e.addError("third", 21);
        assertEquals(List.of("first", "second", "third"), e.getErrorMessages());
        assertEquals(List.of(3, 8, 21), e.getLineNumbers());
    }

    @Test void rejectsNegativeActivityAndCharacterCounts() {
        TelemetryEvent e = new TelemetryEvent("EDITOR_ACTIVITY", t);
        assertThrows(IllegalArgumentException.class, () -> e.setActivityDurationSeconds(-1));
        assertThrows(IllegalArgumentException.class, () -> e.setInsertedCharacters(-1));
        assertThrows(IllegalArgumentException.class, () -> e.setDeletedCharacters(-1));
        assertThrows(IllegalArgumentException.class, () -> e.setIntervalSeconds(-2));
    }

    @Test void rejectsNonFiniteDurations() {
        TelemetryEvent e = new TelemetryEvent("EDITOR_ACTIVITY", t);
        assertThrows(IllegalArgumentException.class, () -> e.setActivityDurationSeconds(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> e.setActivityDurationSeconds(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> e.setIntervalSeconds(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> e.setIntervalSeconds(Double.POSITIVE_INFINITY));
        assertEquals(0, e.getActivityDurationSeconds());
        assertEquals(TelemetryEvent.NO_INTERVAL, e.getIntervalSeconds());
    }

    @Test void acceptsNoIntervalMarkerAndNonNegativeIntervals() {
        TelemetryEvent e = new TelemetryEvent("COMPILE", t);
        e.setIntervalSeconds(0);
        assertEquals(0, e.getIntervalSeconds());
        e.setIntervalSeconds(2.5);
        assertEquals(2.5, e.getIntervalSeconds());
        e.setIntervalSeconds(TelemetryEvent.NO_INTERVAL);
        assertEquals(TelemetryEvent.NO_INTERVAL, e.getIntervalSeconds());
        // -1 is the only negative value that means "no interval"
        assertThrows(IllegalArgumentException.class, () -> e.setIntervalSeconds(-0.5));
        assertEquals(TelemetryEvent.NO_INTERVAL, e.getIntervalSeconds());
    }

    @Test void validatesStatus() {
        TelemetryEvent e = new TelemetryEvent("RUN", t);
        assertThrows(IllegalArgumentException.class, () -> e.setStatus(null));
        assertThrows(IllegalArgumentException.class, () -> e.setStatus("  "));
        assertEquals("IN PROGRESS", e.getStatus());
        e.setStatus("SUCCESS");
        assertEquals("SUCCESS", e.getStatus());
    }

    @Test void returnsUnmodifiableSnapshotsOfErrors() {
        TelemetryEvent e = new TelemetryEvent("COMPILE", t);
        e.addError("Error", 3);

        List<String> messages = e.getErrorMessages();
        List<Integer> lines = e.getLineNumbers();
        assertThrows(UnsupportedOperationException.class, messages::clear);
        assertThrows(UnsupportedOperationException.class, () -> lines.add(7));
        assertEquals(1, e.getErrorMessages().size());
        assertEquals(1, e.getLineNumbers().size());

        e.addError("Another", 4);
        assertEquals(1, messages.size());
        assertEquals(2, e.getErrorMessages().size());
    }

    @Test void storesDescriptiveFields() {
        TelemetryEvent e = new TelemetryEvent("RUN", t);
        e.setProjectName("Demo");
        e.setClassName("Main");
        e.setMethodName("run");
        e.setObjectName("main1");
        e.setErrorCode("E1");
        e.setExceptionMessage("NullPointerException");
        e.setConsoleSummary("trace");
        e.setActivityDurationSeconds(12.5);
        e.setInsertedCharacters(8);
        e.setDeletedCharacters(2);

        assertEquals("Demo", e.getProjectName());
        assertEquals("Main", e.getClassName());
        assertEquals("run", e.getMethodName());
        assertEquals("main1", e.getObjectName());
        assertEquals("E1", e.getErrorCode());
        assertEquals("NullPointerException", e.getExceptionMessage());
        assertEquals("trace", e.getConsoleSummary());
        assertEquals(12.5, e.getActivityDurationSeconds());
        assertEquals(8, e.getInsertedCharacters());
        assertEquals(2, e.getDeletedCharacters());
    }
}
