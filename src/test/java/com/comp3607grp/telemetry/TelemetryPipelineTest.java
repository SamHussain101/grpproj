package com.comp3607grp.telemetry;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.CompileEvent.EventType;

import com.comp3607grp.telemetry.listener.CompilationListener;
import com.comp3607grp.telemetry.report.JsonExporter;
import com.comp3607grp.telemetry.report.ReportExporter;

public class TelemetryPipelineTest {

    @TempDir Path temp;

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 9, 10, 0);
    private static final File[] FILES = { new File("Person.java") };

    private static CompileEvent compile(EventType type) {
        return new CompileEvent(type, true, FILES);
    }

    private static CompileEvent compileError(String message, int line) {
        CompileEvent e = compile(EventType.COMPILE_ERROR_EVENT);
        e.setErrorMessage(message);
        e.setErrorLineNumber(line);
        return e;
    }

    private static JsonObject exportAndParse(ReportExporter exporter, List<TelemetryEvent> events)
            throws Exception {
        List<Path> files = exporter.export(events);
        assertEquals(1, files.size());
        return JsonParser.parseString(Files.readString(files.get(0))).getAsJsonObject();
    }

    @Test void sessionFlowsFromListenerThroughManagerToJsonReport() throws Exception {
        TelemetryManager manager = new TelemetryManager();
        List<TelemetryEvent> observed = new ArrayList<>();
        manager.addObserver(observed::add);

        CompilationListener listener = new CompilationListener(manager);
        listener.compileStarted(compile(EventType.COMPILE_START_EVENT));
        listener.compileError(compileError("';' expected", 12));
        listener.compileError(compileError("';' expected", 30));
        listener.compileFailed(compile(EventType.COMPILE_FAILED_EVENT));
        listener.compileStarted(compile(EventType.COMPILE_START_EVENT));
        listener.compileSucceeded(compile(EventType.COMPILE_DONE_EVENT));

        TelemetryEvent edit = new TelemetryEvent("EDITOR_ACTIVITY", T0);
        edit.setStatus("RECORDED");
        edit.setInsertedCharacters(7);
        edit.setDeletedCharacters(2);
        edit.setActivityDurationSeconds(5);
        manager.recordEvent(edit);

        TelemetryEvent run = new TelemetryEvent("RUN", T0.plusSeconds(1));
        run.setStatus("FAILED");
        run.setExceptionMessage("java.lang.NullPointerException");
        manager.recordEvent(run);

        assertEquals(4, observed.size());

        JsonObject root = exportAndParse(
                new ReportExporter(temp, List.of(new JsonExporter("S001"))), manager.getEvents());
        JsonObject summary = root.getAsJsonObject("summary");
        JsonObject compilation = summary.getAsJsonObject("compilation");

        assertEquals(4, root.getAsJsonObject("session").get("totalEvents").getAsInt());
        assertEquals(2, compilation.get("total").getAsInt());
        assertEquals(1, compilation.get("successful").getAsInt());
        assertEquals(1, compilation.get("failed").getAsInt());
        assertEquals(2, compilation.get("totalErrors").getAsInt());
        assertEquals(2, compilation.getAsJsonObject("errorFrequency").get("';' expected").getAsInt());
        assertEquals(1, summary.getAsJsonObject("execution").get("failed").getAsInt());
        assertEquals(1, summary.getAsJsonObject("activity").get("totalEdits").getAsInt());
        assertEquals(7, summary.getAsJsonObject("activity").get("charactersInserted").getAsInt());
    }

    @Test void jsonEventsCarryTypeSpecificDetails() throws Exception {
        TelemetryEvent edit = new TelemetryEvent("EDITOR_ACTIVITY", T0);
        edit.setStatus("RECORDED");
        edit.setClassName("Person");
        edit.setInsertedCharacters(4);
        edit.setDeletedCharacters(1);

        TelemetryEvent run = new TelemetryEvent("RUN", T0.plusSeconds(10));
        run.setStatus("FAILED");
        run.setExceptionMessage("boom");
        run.setIntervalSeconds(10);

        JsonObject root = exportAndParse(
                new ReportExporter(temp, List.of(new JsonExporter())), List.of(run, edit));
        JsonArray events = root.getAsJsonArray("events");

        JsonObject first = events.get(0).getAsJsonObject();
        assertEquals("EDITOR_ACTIVITY", first.get("type").getAsString());
        assertEquals("Person", first.get("className").getAsString());
        assertEquals(4, first.get("insertedCharacters").getAsInt());
        assertEquals(1, first.get("deletedCharacters").getAsInt());
        assertFalse(first.has("intervalSeconds"));

        JsonObject second = events.get(1).getAsJsonObject();
        assertEquals("RUN", second.get("type").getAsString());
        assertEquals("boom", second.get("exceptionMessage").getAsString());
        assertEquals(10.0, second.get("intervalSeconds").getAsDouble());
    }

    @Test void exportsAnEmptySessionWithoutFailing() throws Exception {
        JsonObject root = exportAndParse(
                new ReportExporter(temp, List.of(new JsonExporter())), List.of());

        assertEquals(0, root.getAsJsonObject("session").get("totalEvents").getAsInt());
        assertEquals(0, root.getAsJsonObject("summary").getAsJsonObject("compilation").get("total").getAsInt());
        assertTrue(root.getAsJsonArray("events").isEmpty());
    }
}
