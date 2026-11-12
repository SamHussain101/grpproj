package com.comp3607grp.telemetry.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.comp3607grp.telemetry.TelemetryEvent;
import com.comp3607grp.telemetry.analytics.ReportAggregator;

public class JsonExporter extends AbstractReportWriter {

    public static final String DEFAULT_STUDENT_ID = "STUDENT_ID";

    static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();

    private final String studentId;

    public JsonExporter() {
        this(DEFAULT_STUDENT_ID);
    }

    public JsonExporter(String studentId) {
        this.studentId = (studentId == null || studentId.isBlank())
                ? DEFAULT_STUDENT_ID : studentId;
    }

    public String toJson(ReportAggregator agg) {
        if (agg == null) {
            throw new IllegalArgumentException("Aggregator cannot be null");
        }
        JsonObject root = new JsonObject();
        root.add("session", buildSession(agg));
        root.add("summary", buildSummary(agg));
        root.add("events", buildEvents(agg.getEvents()));
        return GSON.toJson(root);
    }

    @Override
    public String fileExtension() {
        return "json";
    }

    @Override
    protected void writeReport(ReportAggregator agg, Path file) throws IOException {
        Files.writeString(file, toJson(agg), StandardCharsets.UTF_8);
    }


    private JsonObject buildSession(ReportAggregator agg) {
        JsonObject session = new JsonObject();
        session.addProperty("studentId", studentId);
        session.addProperty("projectName", agg.getProjectName());
        session.addProperty("startTime", format(agg.getSessionStart()));
        session.addProperty("endTime", format(agg.getSessionEnd()));
        session.addProperty("totalSessionDurationSeconds", round(agg.getSessionDurationSeconds()));
        session.addProperty("totalCompilationAttempts", agg.getTotalCompilations());
        session.addProperty("totalEvents", agg.getEvents().size());
        return session;
    }

    private JsonObject buildSummary(ReportAggregator agg) {
        JsonObject activity = new JsonObject();
        activity.addProperty("activeTimeSeconds", round(agg.getActiveTimeSeconds()));
        activity.addProperty("idleTimeSeconds", round(agg.getIdleTimeSeconds()));
        activity.addProperty("activeRatio", round(agg.getActiveRatio()));
        activity.addProperty("totalEdits", agg.getTotalEdits());
        activity.addProperty("charactersInserted", agg.getCharactersInserted());
        activity.addProperty("charactersDeleted", agg.getCharactersDeleted());
        activity.addProperty("editsPerActiveMinute", round(agg.getEditsPerActiveMinute()));

        JsonObject errors = new JsonObject();
        for (Map.Entry<String, Integer> entry : agg.getErrorFrequency().entrySet()) {
            errors.addProperty(entry.getKey(), entry.getValue());
        }

        JsonObject compilation = new JsonObject();
        compilation.addProperty("total", agg.getTotalCompilations());
        compilation.addProperty("successful", agg.getSuccessfulCompilations());
        compilation.addProperty("failed", agg.getFailedCompilations());
        compilation.addProperty("successRate", round(agg.getCompileSuccessRate()));
        compilation.addProperty("totalErrors", agg.getTotalCompileErrors());
        compilation.addProperty("averageSecondsBetweenCompiles",
                round(agg.getAverageSecondsBetweenCompiles()));
        compilation.add("errorFrequency", errors);

        JsonObject execution = new JsonObject();
        execution.addProperty("totalRuns", agg.getTotalRuns());
        execution.addProperty("successful", agg.getSuccessfulRuns());
        execution.addProperty("failed", agg.getFailedRuns());
        execution.addProperty("terminated", agg.getTerminatedRuns());

        JsonObject summary = new JsonObject();
        summary.add("activity", activity);
        summary.add("compilation", compilation);
        summary.add("execution", execution);
        return summary;
    }

    private JsonArray buildEvents(List<TelemetryEvent> events) {
        JsonArray array = new JsonArray();
        for (TelemetryEvent e : events) {
            array.add(buildEvent(e));
        }
        return array;
    }

    JsonObject buildEvent(TelemetryEvent e) {
        JsonObject obj = new JsonObject();
        obj.addProperty("timestamp", format(e.getTimestamp()));
        obj.addProperty("type", e.getEventType());
        obj.addProperty("status", e.getStatus());
        addIfPresent(obj, "projectName", e.getProjectName());
        addIfPresent(obj, "className", e.getClassName());
        addIfPresent(obj, "methodName", e.getMethodName());
        addIfPresent(obj, "objectName", e.getObjectName());

        String type = e.getEventType();
        if (ReportAggregator.COMPILE.equals(type)) {
            addIfPresent(obj, "errorCode", e.getErrorCode());
            obj.add("errors", buildErrors(e));
        } else if (ReportAggregator.EDITOR_ACTIVITY.equals(type)) {
            obj.addProperty("insertedCharacters", e.getInsertedCharacters());
            obj.addProperty("deletedCharacters", e.getDeletedCharacters());
            obj.addProperty("activityDurationSeconds", round(e.getActivityDurationSeconds()));
        } else if (ReportAggregator.RUN.equals(type)) {
            addIfPresent(obj, "exceptionMessage", e.getExceptionMessage());
            addIfPresent(obj, "consoleSummary", e.getConsoleSummary());
        }

        // -1 means no previous event 
        if (e.getIntervalSeconds() >= 0) {
            obj.addProperty("intervalSeconds", round(e.getIntervalSeconds()));
        }
        return obj;
    }

    private JsonArray buildErrors(TelemetryEvent e) {
        JsonArray errors = new JsonArray();
        List<String> messages = e.getErrorMessages();
        List<Integer> lines = e.getLineNumbers();
        for (int i = 0; i < messages.size(); i++) {
            JsonObject error = new JsonObject();
            error.addProperty("message", messages.get(i));
            error.addProperty("line", i < lines.size() ? lines.get(i) : null);
            errors.add(error);
        }
        return errors;
    }

    private static void addIfPresent(JsonObject obj, String key, String value) {
        if (value != null && !value.isBlank()) {
            obj.addProperty(key, value);
        }
    }

    static String format(LocalDateTime time) {
        return time == null ? null : TIMESTAMP.format(time);
    }

    static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
