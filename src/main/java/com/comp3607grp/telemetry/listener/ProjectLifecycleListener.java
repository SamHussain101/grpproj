package com.comp3607grp.telemetry.listener;

import java.time.LocalDateTime;

import bluej.extensions2.BPackage;
import bluej.extensions2.BProject;
import bluej.extensions2.event.PackageEvent;
import bluej.extensions2.event.PackageListener;

import com.comp3607grp.telemetry.EventBuffer;
import com.comp3607grp.telemetry.TelemetryEvent;

public class ProjectLifecycleListener implements PackageListener {

    private final EventBuffer buffer;

    public ProjectLifecycleListener(EventBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("EventBuffer cannot be null");
        }

        this.buffer = buffer;
    }

    @Override
    public void packageOpened(PackageEvent event) {
        recordPackageEvent(event, "PROJECT_OPENED");
    }

    @Override
    public void packageClosing(PackageEvent event) {
        recordPackageEvent(event, "PROJECT_CLOSING");
    }

    private void recordPackageEvent(PackageEvent event, String eventType) {

        BPackage blueJPackage = event.getPackage();

        TelemetryEvent telemetryEvent =
            new TelemetryEvent(eventType, LocalDateTime.now());

        try {
            BProject project = blueJPackage.getProject();

            telemetryEvent.setProjectName(project.getName());
            telemetryEvent.setStatus("SUCCESS");

        } catch (Exception e) {
            telemetryEvent.setStatus("UNKNOWN");
        }

        buffer.addEvent(telemetryEvent);
    }
}
