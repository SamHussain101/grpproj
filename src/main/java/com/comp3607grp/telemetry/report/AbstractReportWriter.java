package com.comp3607grp.telemetry.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.comp3607grp.telemetry.analytics.ReportAggregator;

public abstract class AbstractReportWriter implements ReportWriter {

    @Override
    public final Path write(ReportAggregator agg, Path file) throws IOException {
        validate(agg, file);
        prepareOutput(file);
        writeReport(agg, file);
        return file;
    }

    protected void validate(ReportAggregator agg, Path file) {
        if (agg == null) {
            throw new IllegalArgumentException("Aggregator cannot be null");
        }
        if (file == null) {
            throw new IllegalArgumentException("Output file cannot be null");
        }
    }

    protected void prepareOutput(Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    protected abstract void writeReport(ReportAggregator agg, Path file) throws IOException;
}
