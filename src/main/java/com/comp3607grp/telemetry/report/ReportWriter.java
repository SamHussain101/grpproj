package com.comp3607grp.telemetry.report;

import java.io.IOException;
import java.nio.file.Path;

import com.comp3607grp.telemetry.analytics.ReportAggregator;

public interface ReportWriter {

    String fileExtension();

    Path write(ReportAggregator agg, Path file) throws IOException;
}
