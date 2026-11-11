package com.comp3607grp.telemetry.listener;

import java.time.Duration;
import java.time.LocalDateTime;

import bluej.extensions2.BClass;
import bluej.extensions2.editor.DocumentListener;

import com.comp3607grp.telemetry.TelemetryEvent;
import com.comp3607grp.telemetry.TelemetryManager;

public class EditorActivityListener implements DocumentListener {

    private final TelemetryManager manager;
    private final BClass blueJClass;

    private LocalDateTime previousEditTime;
    private int totalEdits;

    public EditorActivityListener(TelemetryManager manager, BClass blueJClass) {

        if (manager == null || blueJClass == null) {
            throw new IllegalArgumentException(
                "Manager and class cannot be null"
            );
        }

        this.manager = manager;
        this.blueJClass = blueJClass;
        this.previousEditTime = null;
        this.totalEdits = 0;
    }

    @Override
    public synchronized void textReplaced(int offset, String oldText,
            String newText, int position, int length) {

        LocalDateTime now = LocalDateTime.now();

        TelemetryEvent event =
                new TelemetryEvent("EDITOR_ACTIVITY", now);

        event.setClassName(blueJClass.getName());
        event.setStatus("RECORDED");

        double interval = 0;

        if (previousEditTime != null) {
            interval = Duration.between(previousEditTime, now)
                    .toMillis() / 1000.0;

            event.setIntervalSeconds(interval);
        }

        previousEditTime = now;
        totalEdits++;

        int insertedCharacters =
                newText == null ? 0 : newText.length();

        int deletedCharacters =
             oldText == null ? 0 : oldText.length();

        event.setInsertedCharacters(insertedCharacters);
        event.setDeletedCharacters(deletedCharacters);

        if (interval > 0 && interval <= 60) {
            event.setActivityDurationSeconds(interval);
        } else {
            event.setActivityDurationSeconds(0);
        }

        manager.recordEvent(event);
    }
}
