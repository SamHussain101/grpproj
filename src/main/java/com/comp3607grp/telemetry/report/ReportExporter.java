package com.comp3607grp.telemetry.report;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.comp3607grp.telemetry.TelemetryEvent;
import com.comp3607grp.telemetry.analytics.ReportAggregator;

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
        this.outputDir = outputDir;
        this.writers = List.copyOf(writers);
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
