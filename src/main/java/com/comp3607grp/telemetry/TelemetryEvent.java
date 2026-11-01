

package com.comp3607grp.telemetry;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TelemetryEvent {

    private final String eventType;
    private final LocalDateTime timestamp;

    private String status;
    private String errorCode;

    private final List<String> errorMessages;
    private final List<Integer> lineNumbers;

    private double intervalSeconds;

    private String exceptionMessage;
    private String consoleSummary;

    private double activityDurationSeconds;

    private int insertedCharacters;
    private int deletedCharacters;

    private String projectName;
    private String className;

    private String methodName;
    private String objectName;

    

    public TelemetryEvent(
            String eventType,
            LocalDateTime timestamp) {

        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException(
                    "Event type cannot be empty"
            );
        }

        if (timestamp == null) {
            throw new IllegalArgumentException(
                    "Timestamp cannot be null"
            );
        }

        this.eventType = eventType;
        this.timestamp = timestamp;

        this.status = "IN PROGRESS";
        this.errorCode = null;

        this.errorMessages = new ArrayList<>();
        this.lineNumbers = new ArrayList<>();

        this.intervalSeconds = -1;

        this.exceptionMessage = null;
        this.consoleSummary = null;

        this.activityDurationSeconds = 0;

        this.projectName = null;
        this.className = null;

        this.insertedCharacters = 0;
        this.deletedCharacters = 0;
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
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public void addError(String message, int lineNumber) {

        if (message != null && !message.isBlank()) {
            errorMessages.add(message);
            lineNumbers.add(lineNumber);
        }
    }

    public List<String> getErrorMessages() {
        return new ArrayList<>(errorMessages);
    }

    public List<Integer> getLineNumbers() {
        return new ArrayList<>(lineNumbers);
    }

    public double getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(double intervalSeconds) {

        if (intervalSeconds < -1) {
            throw new IllegalArgumentException(
                    "Invalid compilation interval"
            );
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

        if (duration < 0) {
            throw new IllegalArgumentException(
                    "Activity duration cannot be negative"
            );
        }

        this.activityDurationSeconds = duration;
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

    public int getInsertedCharacters() {
    return insertedCharacters;
}

public void setInsertedCharacters(int insertedCharacters) {
    if (insertedCharacters < 0) {
        throw new IllegalArgumentException(
            "Inserted characters cannot be negative"
        );
    }
    this.insertedCharacters = insertedCharacters;
}

public int getDeletedCharacters() {
    return deletedCharacters;
}

public void setDeletedCharacters(int deletedCharacters) {
    if (deletedCharacters < 0) {
        throw new IllegalArgumentException(
            "Deleted characters cannot be negative"
        );
    }
    this.deletedCharacters = deletedCharacters;
}
}
