package com.comp3607grp.telemetry;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.comp3607grp.telemetry.report.JsonExporter;
import com.comp3607grp.telemetry.report.ReportExporter;

public class TelemetryMenuGeneratorTest {

    private final TelemetryManager manager = new TelemetryManager();
    private final ReportExporter exporter =
            new ReportExporter(Path.of("telemetry-test-output"), List.of(new JsonExporter()));

    @Test void rejectsNullManager() {
        assertThrows(IllegalArgumentException.class, () -> new TelemetryMenuGenerator(null, exporter));
    }

    @Test void rejectsNullExporter() {
        assertThrows(IllegalArgumentException.class, () -> new TelemetryMenuGenerator(manager, null));
    }
}
