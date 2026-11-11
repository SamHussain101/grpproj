package com.comp3607grp.telemetry.analytics;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

import com.comp3607grp.telemetry.TelemetryEvent;
import com.comp3607grp.telemetry.TelemetryManager;


public class ReportAggregatorTest {
    private final LocalDateTime t = LocalDateTime.of(2026, 11, 9, 10, 0);
    private TelemetryEvent event(String type, int seconds, String status) {
        TelemetryEvent e = new TelemetryEvent(type, t.plusSeconds(seconds));
        e.setStatus(status);
        return e;
    }

    private TelemetryEvent edit(int seconds, double activeSeconds, int inserted, int deleted) {
        TelemetryEvent e = event("EDITOR_ACTIVITY", seconds, "RECORDED");
        e.setActivityDurationSeconds(activeSeconds);
        e.setInsertedCharacters(inserted);
        e.setDeletedCharacters(deleted);
        return e;
    }

    // ---- Original tests ----

    @Test void calculatesCompilationStatisticsAndErrorFrequency() {
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

    // ---- Required tests ----

    @Test void shouldCalculateCompileSuccessRate() {
        ReportAggregator three = new ReportAggregator(List.of(
                event("COMPILE", 0, "SUCCESS"),
                event("COMPILE", 10, "SUCCESS"),
                event("COMPILE", 20, "SUCCESS"),
                event("COMPILE", 30, "FAILED")));
        assertEquals(0.75, three.getCompileSuccessRate(), 0.0001);

        ReportAggregator oneOfThree = new ReportAggregator(List.of(
                event("COMPILE", 0, "SUCCESS"),
                event("COMPILE", 10, "FAILED"),
                event("COMPILE", 20, "FAILED")));
        assertEquals(1.0 / 3.0, oneOfThree.getCompileSuccessRate(), 0.0001);

        ReportAggregator allFailed = new ReportAggregator(List.of(event("COMPILE", 0, "FAILED")));
        assertEquals(0, allFailed.getCompileSuccessRate(), 0.0001);
    }

    @Test void shouldCountCompilationFailures() {
        ReportAggregator a = new ReportAggregator(List.of(
                event("COMPILE", 0, "SUCCESS"),
                event("COMPILE", 10, "FAILED"),
                event("COMPILE", 20, "FAILED"),
                event("COMPILE", 30, "IN PROGRESS"),
                event("RUN", 40, "FAILED")));      // a failed run is not a failed compilation
        assertEquals(4, a.getTotalCompilations());
        assertEquals(1, a.getSuccessfulCompilations());
        assertEquals(2, a.getFailedCompilations());
    }

    @Test void shouldCalculateErrorFrequency() {
        TelemetryEvent first = event("COMPILE", 0, "FAILED");
        first.addError("Missing symbol", 4);
        first.addError("Missing symbol ", 9);      // trailing space is ignored
        first.addError("Alpha", 12);

        TelemetryEvent second = event("COMPILE", 10, "FAILED");
        second.addError("Missing symbol", 4);
        second.addError("';' expected", 7);

        TelemetryEvent notACompile = event("RUN", 20, "FAILED");
        notACompile.addError("Ignore me", 1);

        ReportAggregator a = new ReportAggregator(List.of(first, second, notACompile));

        assertEquals(3, a.getErrorFrequency().get("Missing symbol"));
        assertEquals(1, a.getErrorFrequency().get("Alpha"));
        assertEquals(1, a.getErrorFrequency().get("';' expected"));
        assertFalse(a.getErrorFrequency().containsKey("Ignore me"));
        assertEquals(5, a.getTotalCompileErrors());
        // most frequent first, ties in alphabetical order
        assertEquals(List.of("Missing symbol", "';' expected", "Alpha"),
                new ArrayList<>(a.getErrorFrequency().keySet()));
    }

    @Test void shouldCalculateActiveAndIdleTime() {
        ReportAggregator a = new ReportAggregator(List.of(
                event("PROJECT_OPENED", 0, "SUCCESS"),
                edit(30, 20, 8, 2),
                edit(60, 10, 4, 1),
                event("PROJECT_CLOSING", 100, "SUCCESS")));

        assertEquals(100, a.getSessionDurationSeconds(), 0.0001);
        assertEquals(30, a.getActiveTimeSeconds(), 0.0001);
        assertEquals(70, a.getIdleTimeSeconds(), 0.0001);
        assertEquals(0.3, a.getActiveRatio(), 0.0001);
        assertEquals(2, a.getTotalEdits());
        assertEquals(12, a.getCharactersInserted());
        assertEquals(3, a.getCharactersDeleted());
        assertEquals(4, a.getEditsPerActiveMinute(), 0.0001);   // 2 edits in 0.5 active minutes
    }

    @Test void shouldHandleEmptyTelemetrySession() {
        ReportAggregator a = new ReportAggregator(List.of());

        assertTrue(a.isEmpty());
        assertTrue(a.getEvents().isEmpty());
        assertNull(a.getSessionStart());
        assertNull(a.getSessionEnd());
        assertNull(a.getProjectName());
        assertEquals(0, a.getSessionDurationSeconds());
        assertEquals(0, a.getActiveTimeSeconds());
        assertEquals(0, a.getIdleTimeSeconds());
        assertEquals(0, a.getActiveRatio());
        assertEquals(0, a.getTotalEdits());
        assertEquals(0, a.getCharactersInserted());
        assertEquals(0, a.getCharactersDeleted());
        assertEquals(0, a.getEditsPerActiveMinute());
        assertEquals(0, a.getTotalCompilations());
        assertEquals(0, a.getSuccessfulCompilations());
        assertEquals(0, a.getFailedCompilations());
        assertEquals(0, a.getCompileSuccessRate());
        assertEquals(0, a.getTotalCompileErrors());
        assertEquals(0, a.getAverageSecondsBetweenCompiles());
        assertTrue(a.getErrorFrequency().isEmpty());
        assertEquals(0, a.getTotalRuns());
        assertEquals(0, a.getSuccessfulRuns());
        assertEquals(0, a.getFailedRuns());
        assertEquals(0, a.getTerminatedRuns());
        assertTrue(a.getClassTimeline().isEmpty());
    }

    // ---- Additional edge cases ----

    @Test void shouldCountInProgressCompilationsInTotalOnly() {
        ReportAggregator a = new ReportAggregator(List.of(
                event("COMPILE", 0, "SUCCESS"),
                event("COMPILE", 10, "IN PROGRESS")));
        assertEquals(2, a.getTotalCompilations());
        assertEquals(1, a.getSuccessfulCompilations());
        assertEquals(0, a.getFailedCompilations());
        assertEquals(0.5, a.getCompileSuccessRate(), 0.0001);
    }

    @Test void shouldCountEachRunOutcomeSeparately() {
        ReportAggregator a = new ReportAggregator(List.of(
                event("RUN", 0, "SUCCESS"),
                event("RUN", 1, "FAILED"),
                event("RUN", 2, "TERMINATED"),
                event("RUN", 3, "UNKNOWN"),
                event("COMPILE", 4, "SUCCESS")));
        assertEquals(4, a.getTotalRuns());
        assertEquals(1, a.getSuccessfulRuns());
        assertEquals(1, a.getFailedRuns());
        assertEquals(1, a.getTerminatedRuns());
    }

    @Test void shouldSortEventsByTimestamp() {
        TelemetryEvent late = event("RUN", 90, "SUCCESS");
        TelemetryEvent early = event("PROJECT_OPENED", 10, "SUCCESS");
        ReportAggregator a = new ReportAggregator(List.of(late, early));

        assertSame(early, a.getEvents().get(0));
        assertSame(late, a.getEvents().get(1));
        assertEquals(t.plusSeconds(10), a.getSessionStart());
        assertEquals(t.plusSeconds(90), a.getSessionEnd());
        assertEquals(80, a.getSessionDurationSeconds(), 0.0001);
    }

    @Test void shouldAverageSecondsBetweenAllCompilations() {
        ReportAggregator a = new ReportAggregator(List.of(
                event("COMPILE", 0, "SUCCESS"),
                event("RUN", 5, "SUCCESS"),             // not a compilation
                event("COMPILE", 10, "FAILED"),
                event("COMPILE", 40, "SUCCESS")));
        assertEquals(20, a.getAverageSecondsBetweenCompiles(), 0.0001);   // gaps of 10 and 30

        ReportAggregator single = new ReportAggregator(List.of(event("COMPILE", 0, "SUCCESS")));
        assertEquals(0, single.getAverageSecondsBetweenCompiles());
    }

    @Test void shouldNeverReportMoreActiveTimeThanTheSessionLasted() {
        ReportAggregator a = new ReportAggregator(List.of(
                event("PROJECT_OPENED", 0, "SUCCESS"),
                edit(5, 25, 1, 0),
                event("PROJECT_CLOSING", 10, "SUCCESS")));
        assertEquals(10, a.getActiveTimeSeconds(), 0.0001);
        assertEquals(0, a.getIdleTimeSeconds(), 0.0001);
        assertEquals(1.0, a.getActiveRatio(), 0.0001);
    }

    @Test void shouldReportZeroDurationForSingleEventSession() {
        ReportAggregator a = new ReportAggregator(List.of(edit(0, 12, 3, 0)));
        assertEquals(0, a.getSessionDurationSeconds());
        assertEquals(0, a.getActiveTimeSeconds());
        assertEquals(0, a.getActiveRatio());
        assertEquals(0, a.getEditsPerActiveMinute());
        assertEquals(1, a.getTotalEdits());
    }

    @Test void shouldUseFirstNonBlankProjectName() {
        TelemetryEvent none = event("RUN", 0, "SUCCESS");
        TelemetryEvent blank = event("RUN", 1, "SUCCESS");
        blank.setProjectName("  ");
        TelemetryEvent named = event("RUN", 2, "SUCCESS");
        named.setProjectName("Demo");
        TelemetryEvent other = event("RUN", 3, "SUCCESS");
        other.setProjectName("Other");

        assertEquals("Demo", new ReportAggregator(List.of(none, blank, named, other)).getProjectName());
    }

    @Test void shouldBuildTimelineWithRenamesRemovalsAndUnknownClasses() {
        TelemetryEvent added = event("CLASS_STATE_CHANGED", 0, "SUCCESS");
        added.setClassName("Main");
        TelemetryEvent repeated = event("CLASS_STATE_CHANGED", 1, "SUCCESS");   // already known: no new entry
        repeated.setClassName("Main");
        TelemetryEvent renamed = event("CLASS_RENAMED", 2, "SUCCESS");
        renamed.setClassName("Renamed");
        TelemetryEvent removed = event("CLASS_REMOVED", 3, "SUCCESS");
        removed.setClassName("Main");
        TelemetryEvent addedAgain = event("CLASS_STATE_CHANGED", 4, "SUCCESS");
        addedAgain.setClassName("Main");
        TelemetryEvent unknownClassMethod = event("METHOD_ADDED", 5, "SUCCESS");
        unknownClassMethod.setMethodName("foo()");
        TelemetryEvent methodRemoved = event("METHOD_REMOVED", 6, "SUCCESS");
        methodRemoved.setClassName("Main");
        methodRemoved.setMethodName("run()");

        List<ReportAggregator.TimelineEntry> timeline = new ReportAggregator(List.of(
                added, repeated, renamed, removed, addedAgain, unknownClassMethod, methodRemoved)).getClassTimeline();

        List<String> actions = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (ReportAggregator.TimelineEntry entry : timeline) {
            actions.add(entry.action());
            names.add(entry.name());
        }
        assertEquals(List.of("Class added", "Class renamed", "Class removed", "Class added",
                "Method added", "Method removed"), actions);
        assertEquals(List.of("Main", "Renamed", "Main", "Main",
                "(unknown class).foo()", "Main.run()"), names);
    }

    @Test void shouldRejectNullEventsAndProtectEventList() {
        assertThrows(IllegalArgumentException.class, () -> new ReportAggregator(null));

        List<TelemetryEvent> source = new ArrayList<>();
        source.add(event("RUN", 0, "SUCCESS"));
        ReportAggregator a = new ReportAggregator(source);
        source.clear();                                  // changing the caller's list does not change the report
        assertEquals(1, a.getEvents().size());
        assertThrows(UnsupportedOperationException.class, () -> a.getEvents().clear());
    }

    @Test void shouldAggregateEventsRecordedByManager() {
        TelemetryManager m = new TelemetryManager();
        m.compileStarted();
        m.compileSucceeded();
        m.compileStarted();
        m.compileError("E1", "Missing symbol", 7);
        m.compileFailed();
        m.recordRun("SUCCESS");
        m.recordRuntimeException("NullPointerException", "trace");
        m.recordEditorActivity("EDITOR_ACTIVITY", "Demo", "Main", 5);

        ReportAggregator a = new ReportAggregator(m.getEvents());

        assertEquals(2, a.getTotalCompilations());
        assertEquals(1, a.getSuccessfulCompilations());
        assertEquals(1, a.getFailedCompilations());
        assertEquals(1, a.getTotalCompileErrors());
        assertEquals(1, a.getErrorFrequency().get("Missing symbol"));
        assertEquals(2, a.getTotalRuns());
        assertEquals(1, a.getSuccessfulRuns());
        assertEquals(1, a.getFailedRuns());
        assertEquals(1, a.getTotalEdits());
        assertEquals("Demo", a.getProjectName());
    }
}
