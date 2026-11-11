package com.comp3607grp.telemetry.report;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.comp3607grp.telemetry.TelemetryEvent;
import com.comp3607grp.telemetry.analytics.ReportAggregator;

public class JsonExporterTest {

    @TempDir Path temp;
    @Test void serializesSessionSummaryAndTimestampedEvents() {
        TelemetryEvent e = new TelemetryEvent("COMPILE", LocalDateTime.of(2026, 11, 9, 10, 0));
        e.setStatus("FAILED");
        e.setProjectName("Demo");
        e.addError("Missing semicolon", 6);

        JsonObject root = JsonParser.parseString(new JsonExporter("S001").toJson(new ReportAggregator(List.of(e)))).getAsJsonObject();
        assertEquals("S001", root.getAsJsonObject("session").get("studentId").getAsString());
        assertEquals(1, root.getAsJsonObject("session").get("totalCompilationAttempts").getAsInt());
        assertEquals(1, root.getAsJsonArray("events").size());

        JsonObject first = root.getAsJsonArray("events").get(0).getAsJsonObject();
        assertTrue(first.has("timestamp"));
        assertEquals("COMPILE", first.get("type").getAsString());
        assertEquals(6, first.getAsJsonArray("errors").get(0).getAsJsonObject().get("line").getAsInt());
    }

    @Test void writesValidJsonFile() throws Exception {
        Path file = temp.resolve("nested/report.json");
        new JsonExporter().write(new ReportAggregator(List.of()), file);
        assertTrue(Files.exists(file));
        assertTrue(JsonParser.parseString(Files.readString(file)).getAsJsonObject().has("events"));
    }

    @Test void rejectsNullAggregator() {
        assertThrows(IllegalArgumentException.class, () -> new JsonExporter().toJson(null));
    }
    
}
