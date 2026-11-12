
package com.comp3607grp.telemetry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.comp3607grp.telemetry.observer.TelemetryObserver;

/**
 * Records and stores telemetry events and notifies observers
 */
public class TelemetryManager {

    private final List<TelemetryEvent> events =
            new ArrayList<>();

   
    private final CopyOnWriteArrayList<TelemetryObserver> observers =
            new CopyOnWriteArrayList<>();

    private LocalDateTime previousCompileTime;
    private TelemetryEvent currentCompile;
    public void addObserver(TelemetryObserver observer) {

        if (observer == null) {
            throw new IllegalArgumentException(
                    "Observer cannot be null"
            );
        }

        observers.addIfAbsent(observer);
    }
    public void removeObserver(TelemetryObserver observer) {

        if (observer != null) {
            observers.remove(observer);
        }
    }

    private void storeAndNotify(TelemetryEvent event) {

        events.add(event);

        for (TelemetryObserver observer : observers) {
            try {
                observer.onEventRecorded(event);
            } catch (RuntimeException e) {
                System.err.println("Telemetry observer failed: " + e);
            }
        }
    }

    public synchronized void recordEvent(
            TelemetryEvent event) {

        if (event == null) {
            throw new IllegalArgumentException(
                    "Event cannot be null"
            );
        }

        storeAndNotify(event);
    }

    public synchronized void recordProjectEvent(
            String eventType, String projectName) {

        TelemetryEvent event = createEvent(eventType);

        event.setProjectName(projectName);
        event.setStatus("SUCCESS");

        storeAndNotify(event);
    }

    public synchronized void recordClassEvent(
            String eventType,
            String projectName,
            String className) {

        TelemetryEvent event = createEvent(eventType);

        event.setProjectName(projectName);
        event.setClassName(className);
        event.setStatus("SUCCESS");

        storeAndNotify(event);
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

        storeAndNotify(event);
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

        storeAndNotify(event);
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

        storeAndNotify(event);
    }

    public synchronized void recordRuntimeException(
            String exceptionMessage,
            String consoleSummary) {

        TelemetryEvent event =
                createEvent("RUN");

        event.setStatus("FAILED");
        event.setExceptionMessage(exceptionMessage);
        event.setConsoleSummary(consoleSummary);

        storeAndNotify(event);
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
}
