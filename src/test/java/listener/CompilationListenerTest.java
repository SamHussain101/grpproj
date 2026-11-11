package com.comp3607grp.telemetry.listener;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.CompileEvent.EventType;

import com.comp3607grp.telemetry.TelemetryEvent;
import com.comp3607grp.telemetry.TelemetryManager;

public class CompilationListenerTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 9, 10, 0);
    private static final File[] FILES = { new File("Person.java") };

    private LocalDateTime now = START;
    private final Supplier<LocalDateTime> clock = () -> now;
    private final TelemetryManager manager = new TelemetryManager();
    private final CompilationListener listener = new CompilationListener(manager, clock);

    private static CompileEvent event(EventType type, boolean user) {
        return new CompileEvent(type, user, FILES);
    }

    private static CompileEvent error(String message, int line) {
        CompileEvent e = event(EventType.COMPILE_ERROR_EVENT, true);
        e.setErrorMessage(message);
        e.setErrorLineNumber(line);
        return e;
    }

    @Test void rejectsNullManager() {
        assertThrows(IllegalArgumentException.class, () -> new CompilationListener(null));
    }

    @Test void acceptsManager() {
        assertNotNull(new CompilationListener(manager));
    }

    @Test void recordsSuccessfulCompilation() {
        listener.compileStarted(event(EventType.COMPILE_START_EVENT, true));
        listener.compileSucceeded(event(EventType.COMPILE_DONE_EVENT, true));

        List<TelemetryEvent> events = manager.getEvents();
        assertEquals(1, events.size());
        assertEquals("COMPILE", events.get(0).getEventType());
        assertEquals("SUCCESS", events.get(0).getStatus());
        assertEquals(START, events.get(0).getTimestamp());
        assertTrue(events.get(0).getErrorMessages().isEmpty());
    }

    @Test void recordsFailedCompilation() {
        listener.compileStarted(event(EventType.COMPILE_START_EVENT, true));
        listener.compileFailed(event(EventType.COMPILE_FAILED_EVENT, true));

        List<TelemetryEvent> events = manager.getEvents();
        assertEquals(1, events.size());
        assertEquals("FAILED", events.get(0).getStatus());
    }

    @Test void showsCompilationErrorDetails() {
        listener.compileStarted(event(EventType.COMPILE_START_EVENT, true));
        listener.compileError(error("';' expected", 12));
        listener.compileError(error("cannot find symbol", 20));
        listener.compileFailed(event(EventType.COMPILE_FAILED_EVENT, true));

        TelemetryEvent recorded = manager.getEvents().get(0);
        assertEquals("FAILED", recorded.getStatus());
        assertEquals(List.of("';' expected", "cannot find symbol"), recorded.getErrorMessages());
        assertEquals(List.of(12, 20), recorded.getLineNumbers());
    }

    @Test void recordsSecondsSincePreviousCompilation() {
        listener.compileStarted(event(EventType.COMPILE_START_EVENT, true));
        listener.compileSucceeded(event(EventType.COMPILE_DONE_EVENT, true));
        now = START.plusSeconds(90);
        listener.compileStarted(event(EventType.COMPILE_START_EVENT, true));
        listener.compileSucceeded(event(EventType.COMPILE_DONE_EVENT, true));

        List<TelemetryEvent> events = manager.getEvents();
        assertEquals(TelemetryEvent.NO_INTERVAL, events.get(0).getIntervalSeconds());
        assertEquals(90.0, events.get(1).getIntervalSeconds());
    }

    @Test void ignoresCompilationsNotStartedByTheUser() {
        listener.compileStarted(event(EventType.COMPILE_START_EVENT, false));
        listener.compileError(error("ignored", 1));
        listener.compileFailed(event(EventType.COMPILE_FAILED_EVENT, false));

        assertTrue(manager.getEvents().isEmpty());
    }

    @Test void ignoresResultWithoutStart() {
        listener.compileSucceeded(event(EventType.COMPILE_DONE_EVENT, true));
        listener.compileError(error("orphan", 3));

        assertTrue(manager.getEvents().isEmpty());
    }

    @Test void ignoresErrorWithoutMessage() {
        listener.compileStarted(event(EventType.COMPILE_START_EVENT, true));
        listener.compileError(error(null, 5));
        listener.compileFailed(event(EventType.COMPILE_FAILED_EVENT, true));

        assertEquals("FAILED", manager.getEvents().get(0).getStatus());
        assertTrue(manager.getEvents().get(0).getErrorMessages().isEmpty());
    }
}
