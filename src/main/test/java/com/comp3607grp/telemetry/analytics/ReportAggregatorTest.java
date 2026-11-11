package com.comp3607grp.telemetry.analytics;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

import com.comp3607grp.telemetry.TelemetryEvent;


public class ReportAggregatorTest {
    private final LocalDateTime t = LocalDateTime.of(2026, 11, 9, 10, 0);
    private TelemetryEvent event(String type, int seconds, String status) {
        TelemetryEvent e = new TelemetryEvent(type, t.plusSeconds(seconds));
        e.setStatus(status);
        return e;
    }

    @test void calculatesCompilationStatisticsAndErrorFrequency() {
        TelemetryEvent good = event("COMPILE", 0, "SUCCESS");
        TelemetryEvent bad = event("COMPILE", 20, "FAILED");
        bad.addError("Missing symbol", 4);
        bad.addError("Missing symbol", 9);

        ReportAggregator a = new ReportAggregator(List.of(bad, good));
        assertEquals(2, a.getTotalCompilations());
        assertEquals(1, a.getSuccessfulCompilations());
        assertEquals(1, a.getFailedCompilations());
        assertEquals(0.5, a.getCompileSuccessRate(), 0.0001);
        assertEquals(2, a.getTotalCompileErrors());
        assertEquals(2, a.getErrorFrequency().get("Missing symbol"));
        assertEquals(20, a.getAverageSecondsBetweenCompiles(), 0.0001);
    }

    @Test void calculatesActiveAndIdleTime() {
        TelemetryEvent edit = event("EDITOR_ACTIVITY", 30, "RECORDED");
        edit.setActivityDurationSeconds(20);
        edit.setInsertedCharacters(8);
        edit.setDeletedCharacters(2);

        ReportAggregator a = new ReportAggregator(List.of(
                event("PROJECT_OPENED", 0, "SUCCESS"),
                edit,
                event("PROJECT_CLOSED", 100, "SUCCESS")
        ));

        assertEquals(100, a.getSessionDurationSeconds(), 0.0001);
        assertEquals(20, a.getActiveTimeSeconds(), 0.0001);
        assertEquals(80, a.getIdleTimeSeconds(), 0.0001);
        assertEquals(0.2, a.getActiveRatio(), 0.0001);
        assertEquals(1, a.getTotalEdits());
        assertEquals(8, a.getCharactersInserted());
        assertEquals(2, a.getCharactersDeleted());
    }

    @Test void handlesEmptySessionWithoutDivisionByZero() {
        ReportAggregator a = new ReportAggregator(List.of());
        assertTrue(a.isEmpty());
        assertEquals(0, a.getCompileSuccessRate());
        assertEquals(0, a.getActiveRatio());
        assertEquals(0, a.getEditsPerActiveMinute());
        assertNull(a.getSessionStart());
    }

    @Test void buildsClassAndMethodTimeline() {
        TelemetryEvent cls = event("CLASS_STATE_CHANGED", 0, "SUCCESS");
        cls.setClassName("Main");

        TelemetryEvent method = event("METHOD_ADDED", 1, "SUCCESS");
        method.setClassName("Main");
        method.setMethodName("run");

        ReportAggregator a = new ReportAggregator(List.of(method, cls));
        assertEquals(2, a.getClassTimeline().size());
        assertEquals("Class added", a.getClassTimeline().get(0).action());
        assertEquals("Main.run", a.getClassTimeline().get(1).name());
    }
}
