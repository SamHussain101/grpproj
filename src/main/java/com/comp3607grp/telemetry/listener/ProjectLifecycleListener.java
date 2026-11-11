package com.comp3607grp.telemetry.listener;

import java.time.LocalDateTime;

import bluej.extensions2.BPackage;
import bluej.extensions2.BProject;
import bluej.extensions2.event.PackageEvent;
import bluej.extensions2.event.PackageListener;

import com.comp3607grp.telemetry.TelemetryEvent;
import com.comp3607grp.telemetry.TelemetryManager;

public class ProjectLifecycleListener implements PackageListener {

    private final TelemetryManager manager;

    public ProjectLifecycleListener(TelemetryManager manager) {
        if (manager == null) {
            throw new IllegalArgumentException("TelemetryManager cannot be null");
        }

        this.manager = manager;
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

        manager.recordEvent(telemetryEvent);
    }
}
