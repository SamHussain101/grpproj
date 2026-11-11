package com.comp3607grp.telemetry.report;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.comp3607grp.telemetry.TelemetryEvent;
import com.comp3607grp.telemetry.analytics.ReportAggregator;

public class ReportExporterTest {
    @TempDir Path temp;

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 9, 10, 0);

    private static TelemetryEvent compileEvent() {
        TelemetryEvent e = new TelemetryEvent("COMPILE", START);
        e.setStatus("SUCCESS");
        return e;
    }

    private static class RecordingWriter implements ReportWriter {
        private final String extension;
        final List<ReportAggregator> received = new ArrayList<>();
        final List<Path> targets = new ArrayList<>();

        RecordingWriter(String extension) {
            this.extension = extension;
        }

        @Override public String fileExtension() {
            return extension;
        }

        @Override public Path write(ReportAggregator agg, Path file) throws IOException {
            received.add(agg);
            targets.add(file);
            Files.writeString(file, "report for " + agg.getEvents().size() + " event(s)");
            return file;
        }
    }

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

    @Test void exportsJsonReport() throws Exception {
        ReportExporter exporter = new ReportExporter(temp, List.of(new JsonExporter()));

        List<Path> files = exporter.export(List.of(compileEvent()));

        assertEquals(1, files.size());
        assertEquals("telemetry-20261109-100000.json", files.get(0).getFileName().toString());
        String content = Files.readString(files.get(0));
        assertTrue(content.contains("\"session\""));
        assertTrue(content.contains("\"events\""));
    }

    @Test void exportsPdfReport() throws Exception {
        ReportExporter exporter = new ReportExporter(temp, List.of(new PdfReportGenerator()));

        List<Path> files = exporter.export(List.of(compileEvent()));

        assertEquals(1, files.size());
        assertEquals("telemetry-20261109-100000.pdf", files.get(0).getFileName().toString());
        byte[] bytes = Files.readAllBytes(files.get(0));
        assertTrue(bytes.length > 4);
        assertEquals("%PDF", new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
    }

    @Test void usesSelectedReportWriter() throws Exception {
        RecordingWriter selected = new RecordingWriter("txt");
        ReportExporter exporter = new ReportExporter(temp, List.of(selected));

        List<Path> files = exporter.export(List.of(compileEvent()));

        assertEquals(1, selected.received.size());
        assertEquals(1, selected.received.get(0).getEvents().size());
        assertEquals(List.of(temp.resolve("telemetry-20261109-100000.txt")), files);
        assertEquals(files, selected.targets);
        try (var listing = Files.list(temp)) {
            assertEquals(1, listing.count());
        }
    }

    @Test void supportsMultipleReportWriters() throws Exception {
        RecordingWriter first = new RecordingWriter("one");
        RecordingWriter second = new RecordingWriter("two");
        ReportExporter exporter = new ReportExporter(temp, List.of(first, second, new JsonExporter()));

        List<Path> files = exporter.export(List.of(compileEvent()));

        assertEquals(3, files.size());
        assertEquals("telemetry-20261109-100000.one", files.get(0).getFileName().toString());
        assertEquals("telemetry-20261109-100000.two", files.get(1).getFileName().toString());
        assertEquals("telemetry-20261109-100000.json", files.get(2).getFileName().toString());
        for (Path path : files) {
            assertTrue(Files.exists(path));
        }
        assertEquals(1, first.received.size());
        assertEquals(1, second.received.size());
        assertSame(first.received.get(0), second.received.get(0));
    }

    @Test void rejectsUnusableWriters() {
        List<ReportWriter> withNull = new ArrayList<>();
        withNull.add(new JsonExporter());
        withNull.add(null);
        assertThrows(IllegalArgumentException.class, () -> new ReportExporter(temp, withNull));
        assertThrows(IllegalArgumentException.class,
                () -> new ReportExporter(temp, List.of(new RecordingWriter("json"), new JsonExporter())));
        assertThrows(IllegalArgumentException.class,
                () -> new ReportExporter(temp, List.of(new RecordingWriter(" "))));
        assertThrows(IllegalArgumentException.class,
                () -> new ReportExporter(temp, List.of(new RecordingWriter(null))));
    }

    @Test void passesWriterFailuresToTheCaller() {
        ReportWriter failing = new ReportWriter() {
            @Override public String fileExtension() { return "bad"; }
            @Override public Path write(ReportAggregator agg, Path file) throws IOException {
                throw new IOException("disk full");
            }
        };
        ReportExporter exporter = new ReportExporter(temp, List.of(failing));
        IOException thrown = assertThrows(IOException.class, () -> exporter.export(List.of(compileEvent())));
        assertEquals("disk full", thrown.getMessage());
    }

    @Test void rejectsNullEventList() {
        ReportExporter exporter = new ReportExporter(temp, List.of(new RecordingWriter("txt")));
        assertThrows(IllegalArgumentException.class, () -> exporter.export(null));
    }
}
