package com.comp3607grp.telemetry.listener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import bluej.extensions2.BClass;
import bluej.extensions2.editor.DocumentListener;

import com.comp3607grp.telemetry.TelemetryEvent;
import com.comp3607grp.telemetry.TelemetryManager;

public class EditorActivityListener implements DocumentListener {

    static final double IDLE_THRESHOLD_SECONDS = 60;

    private final TelemetryManager manager;
    private final String className;
    private final Supplier<LocalDateTime> clock;

    private LocalDateTime previousEditTime;

    public EditorActivityListener(TelemetryManager manager, BClass blueJClass) {
        this(manager, nameOf(manager, blueJClass), LocalDateTime::now);
    }

    EditorActivityListener(TelemetryManager manager, String className,
            Supplier<LocalDateTime> clock) {

        if (manager == null || className == null) {
            throw new IllegalArgumentException(
                "Manager and class cannot be null"
            );
        }

        this.manager = manager;
        this.className = className;
        this.clock = clock;
    }

    private static String nameOf(TelemetryManager manager, BClass blueJClass) {
        if (manager == null || blueJClass == null) {
            throw new IllegalArgumentException(
                "Manager and class cannot be null"
            );
        }
        return blueJClass.getName();
    }

    @Override
    public synchronized void textReplaced(int offset, String oldText,
            String newText, int position, int length) {
        recordEdit(oldText, newText);
    }

    synchronized void recordEdit(String oldText, String newText) {

        LocalDateTime now = clock.get();

        TelemetryEvent event = new TelemetryEvent("EDITOR_ACTIVITY", now);

        event.setClassName(className);
        event.setStatus("RECORDED");

        double interval = 0;

        if (previousEditTime != null) {
            interval = Math.max(0,
                Duration.between(previousEditTime, now).toMillis() / 1000.0);
            event.setIntervalSeconds(interval);
        }

        previousEditTime = now;

        event.setInsertedCharacters(newText == null ? 0 : newText.length());
        event.setDeletedCharacters(oldText == null ? 0 : oldText.length());

        if (interval > 0 && interval <= IDLE_THRESHOLD_SECONDS) {
            event.setActivityDurationSeconds(interval);
        } else {
            event.setActivityDurationSeconds(0);
        }

        manager.recordEvent(event);
    }
}
