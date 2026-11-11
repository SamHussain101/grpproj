package com.comp3607grp.telemetry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ReportExporterTest {
    @TempDir Path temp;
    @Test void exportsJsonAndPdfFiles() throws Exception {
        TelemetryEvent e = new TelemetryEvent("COMPILE", LocalDateTime.of(2026, 11, 9, 10, 0));
        e.setStatus("SUCCESS");
        ReportExporter exporter = new ReportExporter(temp, List.of(new JsonExporter(), new PdfReportGenerator()));
        List<Path> files = exporter.export(List.of(e));
        assertEquals(2, files.size());
        assertTrue(files.stream().anyMatch(p -> p.toString().endsWith(".json")));
        assertTrue(files.stream().anyMatch(p -> p.toString().endsWith(".pdf")));
        for (Path path : files) {
            assertTrue(Files.exists(path));
            assertTrue(Files.size(path) > 0);
        }
    }
    @Test void rejectsMissingOutputFolderAndWriters() {
        assertThrows(IllegalArgumentException.class, () -> new ReportExporter(null, List.of(new JsonExporter())));
        assertThrows(IllegalArgumentException.class, () -> new ReportExporter(temp, List.of()));
    }
}
