package com.comp3607grp.telemetry.analytics;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.comp3607grp.telemetry.TelemetryEvent;

/**
 * Calculates every statistic shown in the menu summary and in the JSON and PDF
 * reports from a list of recorded events. It keeps no counters of its own; each
 * value is derived from the events it was given.
 */
public class ReportAggregator {

    public static final String COMPILE = "COMPILE";
    public static final String RUN = "RUN";
    public static final String EDITOR_ACTIVITY = "EDITOR_ACTIVITY";
    public static final String CLASS_STATE_CHANGED = "CLASS_STATE_CHANGED";
    public static final String CLASS_RENAMED = "CLASS_RENAMED";
    public static final String CLASS_REMOVED = "CLASS_REMOVED";
    public static final String METHOD_ADDED = "METHOD_ADDED";
    public static final String METHOD_REMOVED = "METHOD_REMOVED";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String TERMINATED = "TERMINATED";

    private final List<TelemetryEvent> events;

    public ReportAggregator(List<TelemetryEvent> events) {
        if (events == null) {
            throw new IllegalArgumentException("Events cannot be null");
        }
        List<TelemetryEvent> sorted = new ArrayList<>(events);
        sorted.sort(Comparator.comparing(TelemetryEvent::getTimestamp));
        this.events = Collections.unmodifiableList(sorted);
    }

    public List<TelemetryEvent> getEvents() {
        return events;
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }

    public LocalDateTime getSessionStart() {
        return events.isEmpty() ? null : events.get(0).getTimestamp();
    }

    public LocalDateTime getSessionEnd() {
        return events.isEmpty() ? null : events.get(events.size() - 1).getTimestamp();
    }

    public double getSessionDurationSeconds() {
        if (events.isEmpty()) {
            return 0;
        }
        return Duration.between(getSessionStart(), getSessionEnd()).toMillis() / 1000.0;
    }
    public String getProjectName() {
        for (TelemetryEvent e : events) {
            if (e.getProjectName() != null && !e.getProjectName().isBlank()) {
                return e.getProjectName();
            }
        }
        return null;
    }

    public double getActiveTimeSeconds() {
        double total = ofType(EDITOR_ACTIVITY)
                .map(TelemetryEvent::getActivityDurationSeconds)
                .reduce(0.0, Double::sum);
        return Math.min(total, getSessionDurationSeconds());
    }

    public double getIdleTimeSeconds() {
        return Math.max(0, getSessionDurationSeconds() - getActiveTimeSeconds());
    }

    public double getActiveRatio() {
        double duration = getSessionDurationSeconds();
        return duration == 0 ? 0 : getActiveTimeSeconds() / duration;
    }

    public int getTotalEdits() {
        return count(EDITOR_ACTIVITY, null);
    }

    public int getCharactersInserted() {
        return ofType(EDITOR_ACTIVITY)
                .mapToInt(TelemetryEvent::getInsertedCharacters)
                .sum();
    }

    public int getCharactersDeleted() {
        return ofType(EDITOR_ACTIVITY)
                .mapToInt(TelemetryEvent::getDeletedCharacters)
                .sum();
    }

    public double getEditsPerActiveMinute() {
        double activeMinutes = getActiveTimeSeconds() / 60.0;
        return activeMinutes == 0 ? 0 : getTotalEdits() / activeMinutes;
    }

    public int getTotalCompilations() {
        return count(COMPILE, null);
    }

    public int getSuccessfulCompilations() {
        return count(COMPILE, SUCCESS);
    }

    public int getFailedCompilations() {
        return count(COMPILE, FAILED);
    }

    public double getCompileSuccessRate() {
        int total = getTotalCompilations();
        return total == 0 ? 0 : (double) getSuccessfulCompilations() / total;
    }

    public int getTotalCompileErrors() {
        return ofType(COMPILE)
                .mapToInt(e -> e.getErrorMessages().size())
                .sum();
    }

    public double getAverageSecondsBetweenCompiles() {
        LocalDateTime previous = null;
        double sum = 0;
        int gaps = 0;
        for (TelemetryEvent e : events) {
            if (COMPILE.equals(e.getEventType())) {
                if (previous != null) {
                    sum += Duration.between(previous, e.getTimestamp()).toMillis() / 1000.0;
                    gaps++;
                }
                previous = e.getTimestamp();
            }
        }
        return gaps == 0 ? 0 : sum / gaps;
    }

    public Map<String, Integer> getErrorFrequency() {
        Map<String, Integer> counts = new HashMap<>();
        for (TelemetryEvent e : events) {
            if (COMPILE.equals(e.getEventType())) {
                for (String message : e.getErrorMessages()) {
                    counts.merge(message.trim(), 1, Integer::sum);
                }
            }
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort(Map.Entry.<String, Integer>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey()));
        Map<String, Integer> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : entries) {
            sorted.put(entry.getKey(), entry.getValue());
        }
        return sorted;
    }

    public int getTotalRuns() {
        return count(RUN, null);
    }

    public int getSuccessfulRuns() {
        return count(RUN, SUCCESS);
    }

    public int getFailedRuns() {
        return count(RUN, FAILED);
    }

    public int getTerminatedRuns() {
        return count(RUN, TERMINATED);
    }

    public List<TimelineEntry> getClassTimeline() {
        List<TimelineEntry> timeline = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (TelemetryEvent e : events) {
            String type = e.getEventType();
            String name = e.getClassName() == null ? "(unknown class)" : e.getClassName();
            if (CLASS_STATE_CHANGED.equals(type)) {
                if (seen.add(name)) {
                    timeline.add(new TimelineEntry(e.getTimestamp(), "Class added", name));
                }
            } else if (CLASS_RENAMED.equals(type)) {
                seen.add(name);
                timeline.add(new TimelineEntry(e.getTimestamp(), "Class renamed", name));
            } else if (CLASS_REMOVED.equals(type)) {
                seen.remove(name);
                timeline.add(new TimelineEntry(e.getTimestamp(), "Class removed", name));
            } else if (METHOD_ADDED.equals(type)) {
                timeline.add(new TimelineEntry(e.getTimestamp(), "Method added", name + "." + e.getMethodName()));
            } else if (METHOD_REMOVED.equals(type)) {
                timeline.add(new TimelineEntry(e.getTimestamp(), "Method removed", name + "." + e.getMethodName()));
            }
        }
        return timeline;
    }

    public record TimelineEntry(LocalDateTime timestamp, String action, String name) { }

    private Stream<TelemetryEvent> ofType(String type) {
        return events.stream().filter(e -> type.equals(e.getEventType()));
    }

    private int count(String type, String status) {
        return (int) ofType(type)
                .filter(e -> status == null || status.equals(e.getStatus()))
                .count();
    }
}
