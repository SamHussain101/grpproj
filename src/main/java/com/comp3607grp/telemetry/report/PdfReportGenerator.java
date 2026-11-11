package com.comp3607grp.telemetry.report;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.comp3607grp.telemetry.analytics.ReportAggregator;

public class PdfReportGenerator implements ReportWriter {

    private static final PDType1Font BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    private static final float MARGIN = 50;
    static final int MAX_ERRORS = 10;
    static final int MAX_TIMELINE_ROWS = 40;

    private static final float[] TWO_COLUMNS = {0.6f, 0.4f};

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String studentId;

    public PdfReportGenerator() {
        this(JsonExporter.DEFAULT_STUDENT_ID);
    }

    public PdfReportGenerator(String studentId) {
        this.studentId = (studentId == null || studentId.isBlank())
                ? JsonExporter.DEFAULT_STUDENT_ID : studentId;
    }

    @Override
    public String fileExtension() {
        return "pdf";
    }

    @Override
    public Path write(ReportAggregator agg, Path file) throws IOException {
        if (agg == null) {
            throw new IllegalArgumentException("Aggregator cannot be null");
        }
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (PDDocument doc = new PDDocument()) {
            try (PageWriter w = new PageWriter(doc)) {
                writeTitle(w, agg);
                writeSummary(w, agg);
                writeActivity(w, agg);
                writeCompilations(w, agg);
                writeErrors(w, agg);
                writeTimeline(w, agg);
            }
            doc.save(file.toFile());
        }
        return file;
    }

    private void writeTitle(PageWriter w, ReportAggregator agg) throws IOException {
        w.text("BlueJ Telemetry Session Report", BOLD, 18);
        String project = agg.getProjectName();
        w.text("Student ID: " + studentId, REGULAR, 10);
        w.text("Project: " + (project == null ? "-" : project), REGULAR, 10);
        w.text("Generated: " + DATE_TIME.format(LocalDateTime.now()), REGULAR, 10);
        w.gap(12);
    }

    private void writeSummary(PageWriter w, ReportAggregator agg) throws IOException {
        w.heading("Session Summary");
        w.header(TWO_COLUMNS, "Metric", "Value");
        w.row(TWO_COLUMNS, "Session start", formatTime(agg.getSessionStart(), DATE_TIME));
        w.row(TWO_COLUMNS, "Session end", formatTime(agg.getSessionEnd(), DATE_TIME));
        w.row(TWO_COLUMNS, "Total duration", formatDuration(agg.getSessionDurationSeconds()));
        w.row(TWO_COLUMNS, "Edits made", String.valueOf(agg.getTotalEdits()));
        w.row(TWO_COLUMNS, "Characters typed / deleted",
                agg.getCharactersInserted() + " / " + agg.getCharactersDeleted());
        w.row(TWO_COLUMNS, "Typing cadence",
                String.format("%.1f edits per active minute", agg.getEditsPerActiveMinute()));
        w.row(TWO_COLUMNS, "Program runs (ok / failed / stopped)", agg.getSuccessfulRuns() + " / "
                + agg.getFailedRuns() + " / " + agg.getTerminatedRuns());
        w.gap(16);
    }

    private void writeActivity(PageWriter w, ReportAggregator agg) throws IOException {
        w.heading("Active Coding Time vs Idle Time");
        double active = agg.getActiveTimeSeconds();
        double idle = agg.getIdleTimeSeconds();
        double total = active + idle;
        if (total == 0) {
            w.text("Not enough activity recorded.", REGULAR, 10);
        } else {
            float[] cols = {0.4f, 0.35f, 0.25f};
            w.header(cols, "Type", "Time", "Share");
            w.row(cols, "Active coding", formatDuration(active), formatPercent(active / total));
            w.row(cols, "Idle", formatDuration(idle), formatPercent(idle / total));
            w.gap(6);
            w.splitBar((float) (active / total));
            w.text("Dark = active, light = idle", REGULAR, 9);
        }
        w.gap(16);
    }

    private void writeCompilations(PageWriter w, ReportAggregator agg) throws IOException {
        w.heading("Compilations");
        int total = agg.getTotalCompilations();
        if (total == 0) {
            w.text("No compilations recorded.", REGULAR, 10);
        } else {
            w.header(TWO_COLUMNS, "Metric", "Value");
            w.row(TWO_COLUMNS, "Compilation attempts", String.valueOf(total));
            w.row(TWO_COLUMNS, "Successful", String.valueOf(agg.getSuccessfulCompilations()));
            w.row(TWO_COLUMNS, "Failed", String.valueOf(agg.getFailedCompilations()));
            w.row(TWO_COLUMNS, "Success rate", formatPercent(agg.getCompileSuccessRate()));
            w.row(TWO_COLUMNS, "Total errors reported", String.valueOf(agg.getTotalCompileErrors()));
            w.row(TWO_COLUMNS, "Average time between compiles",
                    formatDuration(agg.getAverageSecondsBetweenCompiles()));
        }
        w.gap(16);
    }

    private void writeErrors(PageWriter w, ReportAggregator agg) throws IOException {
        w.heading("Compilation Error Frequency");
        Map<String, Integer> errors = agg.getErrorFrequency();
        if (errors.isEmpty()) {
            w.text("No compilation errors recorded.", REGULAR, 10);
            w.gap(16);
            return;
        }
        int max = errors.values().iterator().next();
        float[] cols = {0.55f, 0.1f, 0.35f};
        w.header(cols, "Error", "Count", "Frequency");
        int shown = 0;
        for (Map.Entry<String, Integer> entry : errors.entrySet()) {
            if (shown++ >= MAX_ERRORS) {
                break;
            }
            w.barRow(cols, entry.getKey(), String.valueOf(entry.getValue()), (float) entry.getValue() / max);
        }
        if (errors.size() > MAX_ERRORS) {
            w.text("Showing the " + MAX_ERRORS + " most common of " + errors.size()
                    + " error types. See the JSON export for all of them.", REGULAR, 9);
        }
        w.gap(16);
    }

    private void writeTimeline(PageWriter w, ReportAggregator agg) throws IOException {
        w.heading("Class and Method Progression Timeline");
        List<ReportAggregator.TimelineEntry> timeline = agg.getClassTimeline();
        if (timeline.isEmpty()) {
            w.text("No class or method changes recorded this session.", REGULAR, 10);
            return;
        }
        float[] cols = {0.15f, 0.25f, 0.6f};
        w.header(cols, "Time", "Event", "Name");
        int shown = Math.min(timeline.size(), MAX_TIMELINE_ROWS);
        for (int i = 0; i < shown; i++) {
            ReportAggregator.TimelineEntry entry = timeline.get(i);
            w.row(cols, formatTime(entry.timestamp(), TIME), entry.action(), entry.name());
        }
        if (timeline.size() > MAX_TIMELINE_ROWS) {
            w.text("... and " + (timeline.size() - MAX_TIMELINE_ROWS)
                    + " more entries. See the JSON export for the full list.", REGULAR, 9);
        }
    }

    static String formatTime(LocalDateTime time, DateTimeFormatter formatter) {
        return time == null ? "-" : formatter.format(time);
    }

    static String formatDuration(double seconds) {
        long total = Math.round(Math.max(0, seconds));
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        if (h > 0) {
            return String.format("%dh %02dm %02ds", h, m, s);
        }
        if (m > 0) {
            return String.format("%dm %02ds", m, s);
        }
        return s + "s";
    }

    static String formatPercent(double ratio) {
        return String.format("%.1f%%", ratio * 100);
    }

    static String clean(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c == '\n' || c == '\r' || c == '\t') {
                sb.append(' ');
            } else if (c >= 32 && c <= 126) {
                sb.append(c);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    private static final class PageWriter implements Closeable {

        private static final float ROW_HEIGHT = 18;
        private static final float CELL_PADDING = 4;
        private static final float TEXT_SIZE = 10;
        private static final Color DARK = Color.DARK_GRAY;
        private static final Color LIGHT = Color.LIGHT_GRAY;

        private final PDDocument doc;
        private final float width = PDRectangle.A4.getWidth() - 2 * MARGIN;
        private PDPageContentStream cs;
        private float y;

        PageWriter(PDDocument doc) throws IOException {
            this.doc = doc;
            newPage();
        }

        private void newPage() throws IOException {
            if (cs != null) {
                cs.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            cs.setLineWidth(0.5f);
            y = PDRectangle.A4.getHeight() - MARGIN;
        }

        private void ensureSpace(float height) throws IOException {
            if (y - height < MARGIN) {
                newPage();
            }
        }

        void gap(float height) {
            y -= height;
        }

        void heading(String s) throws IOException {
            ensureSpace(13 + 6 + ROW_HEIGHT * 2);
            text(s, BOLD, 13);
            gap(2);
        }

        void text(String s, PDType1Font font, float size) throws IOException {
            ensureSpace(size + 6);
            y -= size + 4;
            write(fit(s, font, size, width), font, size, MARGIN, y);
            y -= 2;
        }

        void header(float[] cols, String... cells) throws IOException {
            row(cols, BOLD, cells);
        }

        void row(float[] cols, String... cells) throws IOException {
            row(cols, REGULAR, cells);
        }

        private void row(float[] cols, PDType1Font font, String... cells) throws IOException {
            ensureSpace(ROW_HEIGHT);
            y -= ROW_HEIGHT;
            float x = MARGIN;
            for (int i = 0; i < cols.length; i++) {
                float colWidth = width * cols[i];
                cs.addRect(x, y, colWidth, ROW_HEIGHT);
                cs.stroke();
                String cell = i < cells.length ? cells[i] : "";
                write(fit(cell, font, TEXT_SIZE, colWidth - 2 * CELL_PADDING), font, TEXT_SIZE,
                        x + CELL_PADDING, y + 5);
                x += colWidth;
            }
        }

        void barRow(float[] cols, String label, String count, float fraction) throws IOException {
            row(cols, label, count, "");
            float barCellX = MARGIN + width * (cols[0] + cols[1]);
            float barMaxWidth = width * cols[2] - 2 * CELL_PADDING;
            fillRect(barCellX + CELL_PADDING, y + 4, barMaxWidth * fraction, ROW_HEIGHT - 8, DARK);
        }

        void splitBar(float leftFraction) throws IOException {
            float height = 16;
            ensureSpace(height + 4);
            y -= height;
            float leftWidth = width * leftFraction;
            fillRect(MARGIN, y, leftWidth, height, DARK);
            fillRect(MARGIN + leftWidth, y, width - leftWidth, height, LIGHT);
            cs.addRect(MARGIN, y, width, height);
            cs.stroke();
        }

        private void fillRect(float x, float yPos, float w, float h, Color color) throws IOException {
            cs.setNonStrokingColor(color);
            cs.addRect(x, yPos, w, h);
            cs.fill();
            cs.setNonStrokingColor(Color.BLACK);
        }

        private void write(String s, PDType1Font font, float size, float x, float yPos) throws IOException {
            cs.beginText();
            cs.setFont(font, size);
            cs.newLineAtOffset(x, yPos);
            cs.showText(s);
            cs.endText();
        }

        private static String fit(String s, PDType1Font font, float size, float maxWidth) throws IOException {
            String text = clean(s);
            if (widthOf(text, font, size) <= maxWidth) {
                return text;
            }
            while (!text.isEmpty() && widthOf(text + "...", font, size) > maxWidth) {
                text = text.substring(0, text.length() - 1);
            }
            return text + "...";
        }

        private static float widthOf(String s, PDType1Font font, float size) throws IOException {
            return font.getStringWidth(s) / 1000 * size;
        }

        @Override
        public void close() throws IOException {
            if (cs != null) {
                cs.close();
                cs = null;
            }
        }
    }
}
