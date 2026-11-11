package com.comp3607grp.telemetry.report;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.comp3607grp.telemetry.TelemetryEvent;
import com.comp3607grp.telemetry.analytics.ReportAggregator;

public class AbstractReportWriterTest {

    @TempDir Path temp;

    private static class RecordingWriter extends AbstractReportWriter {
        final List<String> calls = new ArrayList<>();
        ReportAggregator receivedAggregator;
        Path receivedFile;
        IOException failure;

        @Override public String fileExtension() {
            return "txt";
        }

        @Override protected void validate(ReportAggregator agg, Path file) {
            calls.add("validate");
            super.validate(agg, file);
        }

        @Override protected void prepareOutput(Path file) throws IOException {
            calls.add("prepare");
            super.prepareOutput(file);
        }

        @Override protected void writeReport(ReportAggregator agg, Path file) throws IOException {
            calls.add("write");
            receivedAggregator = agg;
            receivedFile = file;
            if (failure != null) {
                throw failure;
            }
            Files.writeString(file, "content");
        }
    }

    private static ReportAggregator aggregator() {
        TelemetryEvent e = new TelemetryEvent("COMPILE", LocalDateTime.of(2026, 11, 9, 10, 0));
        e.setStatus("SUCCESS");
        return new ReportAggregator(List.of(e));
    }

    @Test void executesTemplateStepsInCorrectOrder() throws Exception {
        RecordingWriter writer = new RecordingWriter();

        writer.write(aggregator(), temp.resolve("report.txt"));

        assertEquals(List.of("validate", "prepare", "write"), writer.calls);
    }

    @Test void invokesSubclassImplementation() throws Exception {
        RecordingWriter writer = new RecordingWriter();
        ReportAggregator agg = aggregator();
        Path file = temp.resolve("report.txt");

        Path result = writer.write(agg, file);

        assertSame(agg, writer.receivedAggregator);
        assertEquals(file, writer.receivedFile);
        assertEquals(file, result);
        assertEquals("content", Files.readString(file));
    }

    @Test void createsOutputDirectoryWhenNeeded() throws Exception {
        RecordingWriter writer = new RecordingWriter();
        Path file = temp.resolve("nested").resolve("deeper").resolve("report.txt");
        assertFalse(Files.exists(file.getParent()));

        writer.write(aggregator(), file);

        assertTrue(Files.isDirectory(file.getParent()));
        assertTrue(Files.exists(file));
    }

    @Test void propagatesReportGenerationFailure() {
        RecordingWriter writer = new RecordingWriter();
        writer.failure = new IOException("disk full");

        IOException thrown = assertThrows(IOException.class,
                () -> writer.write(aggregator(), temp.resolve("report.txt")));

        assertEquals("disk full", thrown.getMessage());
        assertEquals(List.of("validate", "prepare", "write"), writer.calls);
    }

    @Test void rejectsNullAggregatorBeforeAnyOutputIsPrepared() {
        RecordingWriter writer = new RecordingWriter();
        Path file = temp.resolve("nested").resolve("report.txt");

        assertThrows(IllegalArgumentException.class, () -> writer.write(null, file));

        assertEquals(List.of("validate"), writer.calls);
        assertFalse(Files.exists(file.getParent()));
    }

    @Test void rejectsNullOutputFile() {
        RecordingWriter writer = new RecordingWriter();

        assertThrows(IllegalArgumentException.class, () -> writer.write(aggregator(), null));

        assertEquals(List.of("validate"), writer.calls);
    }

    @Test void templateMethodCannotBeOverridden() throws Exception {
        int modifiers = AbstractReportWriter.class
                .getMethod("write", ReportAggregator.class, Path.class)
                .getModifiers();

        assertTrue(Modifier.isFinal(modifiers));
    }

    @Test void jsonAndPdfWritersUseTheTemplate() {
        assertTrue(AbstractReportWriter.class.isAssignableFrom(JsonExporter.class));
        assertTrue(AbstractReportWriter.class.isAssignableFrom(PdfReportGenerator.class));
        assertTrue(ReportWriter.class.isAssignableFrom(AbstractReportWriter.class));
    }
}
