package com.comp3607grp.telemetry.observer;

import com.comp3607grp.telemetry.TelemetryEvent;

/**
 * Observer in the Observer pattern. The TelemetryManager is the subject.
 *
 */
@FunctionalInterface
public interface TelemetryObserver {

    /** Called after the event has been stored by the manager. */
    void onEventRecorded(TelemetryEvent event);
}
