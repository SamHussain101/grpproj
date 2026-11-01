
package com.comp3607grp.telemetry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TelemetryManager {

    private final List<TelemetryEvent> events =
            new ArrayList<>();

    private LocalDateTime previousCompileTime;
    private TelemetryEvent currentCompile;

    public synchronized void recordEvent(
            TelemetryEvent event) {

        if (event == null) {
            throw new IllegalArgumentException(
                    "Event cannot be null"
            );
        }

        events.add(event);
    }

    public synchronized void recordProjectEvent(
            String eventType, String projectName) {

        TelemetryEvent event = createEvent(eventType);

        event.setProjectName(projectName);
        event.setStatus("SUCCESS");

        events.add(event);
    }

    public synchronized void recordClassEvent(
            String eventType,
            String projectName,
            String className) {

        TelemetryEvent event = createEvent(eventType);

        event.setProjectName(projectName);
        event.setClassName(className);
        event.setStatus("SUCCESS");

        events.add(event);
    }

    public synchronized void recordEditorActivity(
            String activityType,
            String projectName,
            String className,
            double durationSeconds) {

        TelemetryEvent event = createEvent(activityType);

        event.setProjectName(projectName);
        event.setClassName(className);
        event.setActivityDurationSeconds(durationSeconds);
        event.setStatus("RECORDED");

        events.add(event);
    }

    public synchronized void compileStarted() {

        LocalDateTime now = LocalDateTime.now();

        TelemetryEvent event =
                new TelemetryEvent("COMPILE", now);

        if (previousCompileTime != null) {

            double interval = Duration.between(
                    previousCompileTime, now
            ).toMillis() / 1000.0;

            event.setIntervalSeconds(interval);
        }

        previousCompileTime = now;
        currentCompile = event;

        events.add(event);
    }

    public synchronized void compileError(
            String errorCode,
            String message,
            int lineNumber) {

        if (currentCompile != null) {

            currentCompile.setErrorCode(errorCode);
            currentCompile.addError(message, lineNumber);
        }
    }

    public synchronized void compileSucceeded() {

        if (currentCompile != null) {

            currentCompile.setStatus("SUCCESS");
            currentCompile = null;
        }
    }

    public synchronized void compileFailed() {

        if (currentCompile != null) {

            currentCompile.setStatus("FAILED");
            currentCompile = null;
        }
    }

    public synchronized void recordRun(String status) {

        TelemetryEvent event =
                createEvent("RUN");

        event.setStatus(status);

        events.add(event);
    }

    public synchronized void recordRuntimeException(
            String exceptionMessage,
            String consoleSummary) {

        TelemetryEvent event =
                createEvent("RUN");

        event.setStatus("FAILED");
        event.setExceptionMessage(exceptionMessage);
        event.setConsoleSummary(consoleSummary);

        events.add(event);
    }

    private TelemetryEvent createEvent(String eventType) {

        return new TelemetryEvent(
                eventType,
                LocalDateTime.now()
        );
    }

    public synchronized List<TelemetryEvent> getEvents() {

        return new ArrayList<>(events);
    }


    public synchronized int getTotalCompilations() {

        return (int) events.stream()
                .filter(e -> e.getEventType().equals("COMPILE"))
                .count();
    }

    public synchronized int getSuccessfulCompilations() {

        return (int) events.stream()
                .filter(e -> e.getEventType().equals("COMPILE")
                        && e.getStatus().equals("SUCCESS"))
                .count();
    }

    public synchronized int getFailedCompilations() {

        return (int) events.stream()
                .filter(e -> e.getEventType().equals("COMPILE")
                        && e.getStatus().equals("FAILED"))
                .count();
    }

    public synchronized int getTotalErrors() {

        return events.stream()
                .filter(e -> e.getEventType().equals("COMPILE"))
                .mapToInt(e -> e.getErrorMessages().size())
                .sum();
    }

    public synchronized int getTotalRuns() {

        return (int) events.stream()
                .filter(e -> e.getEventType().equals("RUN"))
                .count();
    }

    public synchronized int getSuccessfulRuns() {

        return (int) events.stream()
                .filter(e -> e.getEventType().equals("RUN")
                        && e.getStatus().equals("SUCCESS"))
                .count();
    }

    public synchronized int getFailedRuns() {

        return (int) events.stream()
                .filter(e -> e.getEventType().equals("RUN")
                        && e.getStatus().equals("FAILED"))
                .count();
    }

    public synchronized int getTotalProjectEvents() {

        return (int) events.stream()
                .filter(e -> e.getEventType().startsWith("PROJECT_"))
                .count();
    }

    public synchronized int getTotalClassEvents() {

        return (int) events.stream()
                .filter(e -> e.getEventType().startsWith("CLASS_"))
                .count();
    }


    public synchronized double getTotalEditorActivitySeconds() {

        return events.stream()
                .filter(e -> e.getEventType().equals("EDITOR_ACTIVITY"))
                .mapToDouble(TelemetryEvent::getActivityDurationSeconds)
                .sum();
    }

    public synchronized int getTotalEditorActivities() {

        return (int) events.stream()
                .filter(e -> e.getEventType().equals("EDITOR_ACTIVITY"))
                .count();
    }

    public synchronized void printSummary() {
        System.out.println("Total events: " + events.size());
        System.out.println("Total compilations: " + getTotalCompilations());
        System.out.println("Successful compilations: " + getSuccessfulCompilations());
        System.out.println("Failed compilations: " + getFailedCompilations());
        System.out.println("Total errors: " + getTotalErrors());
        System.out.println("Total runs: " + getTotalRuns());
    }
}
