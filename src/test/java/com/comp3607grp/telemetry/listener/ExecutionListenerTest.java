package com.comp3607grp.telemetry.listener;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import bluej.extensions2.event.InvocationFinishedEvent.EventType;

import com.comp3607grp.telemetry.TelemetryEvent;
import com.comp3607grp.telemetry.TelemetryManager;

public class ExecutionListenerTest {

    private static final LocalDateTime WHEN = LocalDateTime.of(2026, 11, 9, 10, 0);

    private static TelemetryEvent map(EventType type, Object result) {
        return ExecutionListener.toTelemetryEvent(type, "Person", "getName", "person1", result, WHEN);
    }

    @Test void rejectsNullManager() {
        assertThrows(IllegalArgumentException.class, () -> new ExecutionListener(null));
    }

    @Test void acceptsManager() {
        assertNotNull(new ExecutionListener(new TelemetryManager()));
    }

    @Test void recordsSuccessfulExecution() {
        TelemetryEvent e = map(EventType.NORMAL_EXIT, "Ann");

        assertEquals("RUN", e.getEventType());
        assertEquals("SUCCESS", e.getStatus());
        assertEquals(WHEN, e.getTimestamp());
        assertEquals("Person", e.getClassName());
        assertEquals("getName", e.getMethodName());
        assertEquals("person1", e.getObjectName());
        assertNull(e.getExceptionMessage());
    }

    @Test void recordsRecordRuntimeException() {
        TelemetryEvent e = map(EventType.EXCEPTION_EXIT, "java.lang.NullPointerException: boom");

        assertEquals("FAILED", e.getStatus());
        assertEquals("java.lang.NullPointerException: boom", e.getExceptionMessage());
    }

    @Test void recordsFailureWithoutExceptionDetailsWhenResultIsMissing() {
        TelemetryEvent e = map(EventType.EXCEPTION_EXIT, null);

        assertEquals("FAILED", e.getStatus());
        assertNull(e.getExceptionMessage());
    }

    @Test void mapsTerminatedAndUnknownExits() {
        assertEquals("TERMINATED", map(EventType.TERMINATED_EXIT, null).getStatus());
        assertEquals("UNKNOWN", map(EventType.UNKNOWN_EXIT, null).getStatus());
    }

    @Test void keepsStaticCallAndConstructorNamesAsNull() {
        TelemetryEvent e = ExecutionListener.toTelemetryEvent(
                EventType.NORMAL_EXIT, "Person", null, null, null, WHEN);

        assertNull(e.getMethodName());
        assertNull(e.getObjectName());
    }
}
