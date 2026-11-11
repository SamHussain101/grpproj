package com.comp3607grp.telemetry.report;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import com.comp3607grp.telemetry.TelemetryEvent;
import com.comp3607grp.telemetry.analytics.ReportAggregator;

public class PdfReportGeneratorTest {

    @TempDir Path temp;

    private static ReportAggregator sampleSession() {
        TelemetryEvent compile = new TelemetryEvent("COMPILE", LocalDateTime.of(2026, 11, 9, 10, 0));
        compile.setStatus("FAILED");
        compile.setProjectName("Demo");
        compile.addError("Missing semicolon", 6);
        TelemetryEvent run = new TelemetryEvent("RUN", LocalDateTime.of(2026, 11, 9, 10, 5));
        run.setStatus("SUCCESS");
        return new ReportAggregator(List.of(compile, run));
    }

    @Test void writesReadablePdfWithAtLeastOnePage() throws Exception {
        Path file = temp.resolve("nested").resolve("report.pdf");

        Path written = new PdfReportGenerator().write(sampleSession(), file);

        assertEquals(file, written);
        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            assertTrue(document.getNumberOfPages() >= 1);
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("BlueJ Telemetry Session Report"));
            assertTrue(text.contains("Demo"));
        }
    }

    @Test void writesPdfForAnEmptySession() throws Exception {
        Path file = temp.resolve("empty.pdf");

        new PdfReportGenerator().write(new ReportAggregator(List.of()), file);

        assertTrue(Files.size(file) > 4);
    }

    @Test void usesPdfExtensionAndRejectsNullAggregator() {
        PdfReportGenerator generator = new PdfReportGenerator();

        assertEquals("pdf", generator.fileExtension());
        assertThrows(IllegalArgumentException.class, () -> generator.write(null, temp.resolve("x.pdf")));
    }
}
