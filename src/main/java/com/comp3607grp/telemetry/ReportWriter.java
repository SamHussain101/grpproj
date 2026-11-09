package com.comp3607grp.telemetry;

import java.io.IOException;
import java.nio.file.Path;

public interface ReportWriter {

    String fileExtension();

    Path write(ReportAggregator agg, Path file) throws IOException;
}
