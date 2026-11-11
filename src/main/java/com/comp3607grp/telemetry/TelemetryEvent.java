package com.comp3607grp.telemetry;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One telemetry record: a compilation, a program run, an editor change, or a
 * project/class/method lifecycle change. Only the fields that apply to an event
 * type are normally filled in; the rest keep their defaults.
 */
public class TelemetryEvent {

    /** Status of an event that has not been completed yet. */
    public static final String DEFAULT_STATUS = "IN PROGRESS";

    /** Value of {@link #getIntervalSeconds()} when there is no previous event. */
    public static final double NO_INTERVAL = -1;

    private final String eventType;
    private final LocalDateTime timestamp;

    private String status = DEFAULT_STATUS;

    // Compilation details. Message and line number are always stored together.
    private String errorCode;
    private final List<CompileError> errors = new ArrayList<>();

    private double intervalSeconds = NO_INTERVAL;

    // Execution details
    private String exceptionMessage;
    private String consoleSummary;

    // Editor activity details
    private double activityDurationSeconds;
    private int insertedCharacters;
    private int deletedCharacters;

    // Location details
    private String projectName;
    private String className;
    private String methodName;
    private String objectName;

    private record CompileError(String message, int lineNumber) { }

    public TelemetryEvent(String eventType, LocalDateTime timestamp) {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("Event type cannot be empty");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        this.eventType = eventType;
        this.timestamp = timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status cannot be empty");
        }
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /** Records a compilation error. Null or blank messages are ignored. */
    public void addError(String message, int lineNumber) {
        if (message != null && !message.isBlank()) {
            errors.add(new CompileError(message, lineNumber));
        }
    }

    /** Returns an unmodifiable snapshot; later errors do not change it. */
    public List<String> getErrorMessages() {
        return errors.stream().map(CompileError::message).toList();
    }

    /** Returns an unmodifiable snapshot, in the same order as the messages. */
    public List<Integer> getLineNumbers() {
        return errors.stream().map(CompileError::lineNumber).toList();
    }

    /** Seconds since the previous event of the same kind, or {@link #NO_INTERVAL}. */
    public double getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(double intervalSeconds) {
        if (intervalSeconds != NO_INTERVAL) {
            requireNonNegative(intervalSeconds, "Interval cannot be negative");
        }
        this.intervalSeconds = intervalSeconds;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public String getConsoleSummary() {
        return consoleSummary;
    }

    public void setConsoleSummary(String consoleSummary) {
        this.consoleSummary = consoleSummary;
    }

    public double getActivityDurationSeconds() {
        return activityDurationSeconds;
    }

    public void setActivityDurationSeconds(double duration) {
        requireNonNegative(duration, "Activity duration cannot be negative");
        this.activityDurationSeconds = duration;
    }

    public int getInsertedCharacters() {
        return insertedCharacters;
    }

    public void setInsertedCharacters(int insertedCharacters) {
        if (insertedCharacters < 0) {
            throw new IllegalArgumentException("Inserted characters cannot be negative");
        }
        this.insertedCharacters = insertedCharacters;
    }

    public int getDeletedCharacters() {
        return deletedCharacters;
    }

    public void setDeletedCharacters(int deletedCharacters) {
        if (deletedCharacters < 0) {
            throw new IllegalArgumentException("Deleted characters cannot be negative");
        }
        this.deletedCharacters = deletedCharacters;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    private static void requireNonNegative(double value, String message) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
