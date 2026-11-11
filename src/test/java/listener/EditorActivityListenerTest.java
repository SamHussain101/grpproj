package com.comp3607grp.telemetry.listener;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.comp3607grp.telemetry.TelemetryEvent;
import com.comp3607grp.telemetry.TelemetryManager;
import com.comp3607grp.telemetry.analytics.ReportAggregator;

public class EditorActivityListenerTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 9, 10, 0);

    private LocalDateTime now = START;
    private final TelemetryManager manager = new TelemetryManager();
    private final EditorActivityListener listener =
            new EditorActivityListener(manager, "Person", () -> now);

    private void editAfter(long seconds, String oldText, String newText) {
        now = now.plusSeconds(seconds);
        listener.textReplaced(0, oldText, newText, 0, 0);
    }

    @Test void rejectsNullManagerOrClass() {
        assertThrows(IllegalArgumentException.class,
                () -> new EditorActivityListener(null, (bluej.extensions2.BClass) null));
        assertThrows(IllegalArgumentException.class,
                () -> new EditorActivityListener(manager, (bluej.extensions2.BClass) null));
        assertThrows(IllegalArgumentException.class,
                () -> new EditorActivityListener(null, "Person", () -> now));
        assertThrows(IllegalArgumentException.class,
                () -> new EditorActivityListener(manager, (String) null, () -> now));
    }

    @Test void recordsEditorModification() {
        editAfter(0, "ab", "abcde");

        List<TelemetryEvent> events = manager.getEvents();
        assertEquals(1, events.size());
        TelemetryEvent e = events.get(0);
        assertEquals("EDITOR_ACTIVITY", e.getEventType());
        assertEquals("RECORDED", e.getStatus());
        assertEquals("Person", e.getClassName());
        assertEquals(5, e.getInsertedCharacters());
        assertEquals(2, e.getDeletedCharacters());
        assertEquals(TelemetryEvent.NO_INTERVAL, e.getIntervalSeconds());
        assertEquals(0.0, e.getActivityDurationSeconds());
    }

    @Test void treatsMissingTextAsZeroCharacters() {
        editAfter(0, null, null);

        assertEquals(0, manager.getEvents().get(0).getInsertedCharacters());
        assertEquals(0, manager.getEvents().get(0).getDeletedCharacters());
    }

    @Test void calculatesActiveTimeBetweenEdits() {
        editAfter(0, "", "a");
        editAfter(10, "", "b");
        editAfter(20, "", "c");

        List<TelemetryEvent> events = manager.getEvents();
        assertEquals(10.0, events.get(1).getIntervalSeconds());
        assertEquals(10.0, events.get(1).getActivityDurationSeconds());
        assertEquals(20.0, events.get(2).getActivityDurationSeconds());
        ReportAggregator agg = new ReportAggregator(events);
        assertEquals(30.0, agg.getActiveTimeSeconds());
        assertEquals(0.0, agg.getIdleTimeSeconds());
    }

    @Test void listensIdleTimeAfterThreshold() {
        editAfter(0, "", "a");
        editAfter(30, "", "b");
        editAfter(120, "", "c");

        List<TelemetryEvent> events = manager.getEvents();
        assertEquals(120.0, events.get(2).getIntervalSeconds());
        assertEquals(0.0, events.get(2).getActivityDurationSeconds());
        ReportAggregator agg = new ReportAggregator(events);
        assertEquals(30.0, agg.getActiveTimeSeconds());
        assertEquals(150.0, agg.getSessionDurationSeconds());
        assertEquals(120.0, agg.getIdleTimeSeconds());
    }

    @Test void gapEqualToThresholdStillCountsAsActive() {
        editAfter(0, "", "a");
        editAfter(60, "", "b");

        assertEquals(60.0, manager.getEvents().get(1).getActivityDurationSeconds());
    }
}
