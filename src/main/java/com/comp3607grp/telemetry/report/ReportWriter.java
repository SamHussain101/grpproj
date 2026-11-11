package com.comp3607grp.telemetry.report;

import java.io.IOException;
import java.nio.file.Path;

import com.comp3607grp.telemetry.analytics.ReportAggregator;

/**
 * Strategy interface for report formats. Each implementation (JSON, PDF, ...)
 * turns the same {@link ReportAggregator} into one file in its own format.
 * {@link ReportExporter} works only with this interface, so a new format can be
 * added without changing the exporter.
 *
 * <p>Implementations should read statistics from the aggregator and not
 * recalculate them, create the target folder if it is missing, and reject a
 * null aggregator with an {@link IllegalArgumentException}.
 */
public interface ReportWriter {

    /** File extension without the dot, for example "json". Must not be blank. */
    String fileExtension();

    /** Writes the report to the given file and returns the file that was written. */
    Path write(ReportAggregator agg, Path file) throws IOException;
}
