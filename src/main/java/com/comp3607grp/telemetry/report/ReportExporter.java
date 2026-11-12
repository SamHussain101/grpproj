package com.comp3607grp.telemetry.report;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.comp3607grp.telemetry.TelemetryEvent;
import com.comp3607grp.telemetry.analytics.ReportAggregator;

/**
 * Context in the Strategy pattern. It calculates the statistics once, then
 * hands the same {@link ReportAggregator} to every {@link ReportWriter} it was
 * given. It knows nothing about JSON or PDF, only the ReportWriter interface.
 */
public class ReportExporter {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path outputDir;
    private final List<ReportWriter> writers;

    public ReportExporter(Path outputDir, List<ReportWriter> writers) {
        if (outputDir == null) {
            throw new IllegalArgumentException("Output folder cannot be null");
        }
        if (writers == null || writers.isEmpty()) {
            throw new IllegalArgumentException("At least one report writer is required");
        }
        requireUsableWriters(writers);
        this.outputDir = outputDir;
        this.writers = List.copyOf(writers);
    }

    // Two writers with the same extension would overwrite each other's file.
    private static void requireUsableWriters(List<ReportWriter> writers) {
        Set<String> extensions = new HashSet<>();
        for (ReportWriter writer : writers) {
            if (writer == null) {
                throw new IllegalArgumentException("Report writer cannot be null");
            }
            String extension = writer.fileExtension();
            if (extension == null || extension.isBlank()) {
                throw new IllegalArgumentException("Report writer must provide a file extension");
            }
            if (!extensions.add(extension.toLowerCase())) {
                throw new IllegalArgumentException(
                        "Two report writers use the same file extension: " + extension);
            }
        }
    }

    public List<Path> export(List<TelemetryEvent> events) throws IOException {
        ReportAggregator agg = new ReportAggregator(events);
        LocalDateTime stamp = agg.getSessionStart() == null ? LocalDateTime.now() : agg.getSessionStart();
        String base = "telemetry-" + FILE_STAMP.format(stamp);
        List<Path> files = new ArrayList<>();
        for (ReportWriter writer : writers) {
            files.add(writer.write(agg, outputDir.resolve(base + "." + writer.fileExtension())));
        }
        return files;
    }

    public Path getOutputDir() {
        return outputDir;
    }
}
