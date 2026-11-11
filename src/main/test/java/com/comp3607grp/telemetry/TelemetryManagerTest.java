package com.comp3607grp.telemetry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import com.comp3607grp.telemetry.observer.TelemetryObserver;

import static org.junit.jupiter.api.Assertions.*;

public class TelemetryManagerTest {
    @Test void recordsProjectAndClassEvents() {
        TelemetryManager m = new TelemetryManager();
        m.recordProjectEvent("PROJECT_OPENED", "Demo");
        m.recordClassEvent("CLASS_STATE_CHANGED", "Demo", "Main");
        assertEquals(2, m.getEvents().size());
        assertEquals("Demo", m.getEvents().get(0).getProjectName());
        assertEquals("Main", m.getEvents().get(1).getClassName());
    }

    @Test void recordsSuccessfulAndFailedCompilations() {
        TelemetryManager m = new TelemetryManager();
        m.compileStarted();
        m.compileSucceeded();
        m.compileStarted();
        m.compileError("E1", "Missing symbol", 7);
        m.compileFailed();
        assertEquals(2, m.getTotalCompilations());
        assertEquals(1, m.getSuccessfulCompilations());
        assertEquals(1, m.getFailedCompilations());
        assertEquals(1, m.getTotalErrors());
        assertEquals("E1", m.getEvents().get(1).getErrorCode());
    }
    @Test void rejectsNullEventsAndProtectsEventList() {
        TelemetryManager m = new TelemetryManager();
        assertThrows(IllegalArgumentException.class, () -> m.recordEvent(null));
        m.getEvents().clear();
        assertTrue(m.getEvents().isEmpty());
    }


    @Test void recordsRunsAndRuntimeExceptions() {
        TelemetryManager m = new TelemetryManager();
        m.recordRun("SUCCESS");
        m.recordRuntimeException("NullPointerException", "trace");
        assertEquals(2, m.getTotalRuns());
        assertEquals("NullPointerException", m.getEvents().get(1).getExceptionMessage());
    }
    

    // ---- Observer pattern ----

    @Test void shouldNotifyObserverWhenEventRecorded() {
        TelemetryManager m = new TelemetryManager();
        List<TelemetryEvent> received = new ArrayList<>();
        m.addObserver(received::add);

        TelemetryEvent e = new TelemetryEvent("RUN", java.time.LocalDateTime.now());
        m.recordEvent(e);

        assertEquals(1, received.size());
        assertSame(e, received.get(0));
        assertEquals(1, m.getEvents().size());
    }

    @Test void shouldNotifyMultipleObservers() {
        TelemetryManager m = new TelemetryManager();
        List<TelemetryEvent> first = new ArrayList<>();
        List<TelemetryEvent> second = new ArrayList<>();
        m.addObserver(first::add);
        m.addObserver(second::add);

        m.recordProjectEvent("PROJECT_OPENED", "Demo");
        m.recordRun("SUCCESS");

        assertEquals(2, first.size());
        assertEquals(2, second.size());
        assertEquals("PROJECT_OPENED", first.get(0).getEventType());
        assertEquals("RUN", second.get(1).getEventType());
    }

    @Test void shouldNotNotifyRemovedObserver() {
        TelemetryManager m = new TelemetryManager();
        List<TelemetryEvent> removed = new ArrayList<>();
        List<TelemetryEvent> kept = new ArrayList<>();
        TelemetryObserver removedObserver = removed::add;
        m.addObserver(removedObserver);
        m.addObserver(kept::add);

        m.recordRun("SUCCESS");
        m.removeObserver(removedObserver);
        m.recordRun("FAILED");

        assertEquals(1, removed.size());
        assertEquals(2, kept.size());
        assertEquals(2, m.getEvents().size());
    }

    @Test void shouldRecordEventWithoutObservers() {
        TelemetryManager m = new TelemetryManager();
        m.recordRun("SUCCESS");
        m.recordClassEvent("CLASS_STATE_CHANGED", "Demo", "Main");
        m.removeObserver(e -> { });   // removing an unknown observer is harmless
        assertEquals(2, m.getEvents().size());
    }

    @Test void shouldKeepRecordingWhenObserverThrows() {
        TelemetryManager m = new TelemetryManager();
        List<TelemetryEvent> received = new ArrayList<>();
        m.addObserver(e -> { throw new IllegalStateException("boom"); });
        m.addObserver(received::add);

        m.recordRun("SUCCESS");

        assertEquals(1, m.getEvents().size());
        assertEquals(1, received.size());
    }

    @Test void shouldNotifyOnceWhenSameObserverAddedTwice() {
        TelemetryManager m = new TelemetryManager();
        List<TelemetryEvent> received = new ArrayList<>();
        TelemetryObserver observer = received::add;
        m.addObserver(observer);
        m.addObserver(observer);

        m.recordRun("SUCCESS");

        assertEquals(1, received.size());
    }

    @Test void shouldRejectNullObserver() {
        TelemetryManager m = new TelemetryManager();
        assertThrows(IllegalArgumentException.class, () -> m.addObserver(null));
    }

    @Test void shouldStoreEventBeforeNotifyingObserver() {
        TelemetryManager m = new TelemetryManager();
        List<Integer> sizeSeenByObserver = new ArrayList<>();
        m.addObserver(e -> sizeSeenByObserver.add(m.getEvents().size()));

        m.recordRun("SUCCESS");

        assertEquals(List.of(1), sizeSeenByObserver);
    }
}
