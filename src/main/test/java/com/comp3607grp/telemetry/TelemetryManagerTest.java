package com.comp3607grp.telemetry;
import org.junit.jupiter.api.Test;
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
    
}
