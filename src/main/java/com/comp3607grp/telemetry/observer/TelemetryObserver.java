package com.comp3607grp.telemetry.observer;

import com.comp3607grp.telemetry.TelemetryEvent;

/**
 * Observer in the Observer pattern. The TelemetryManager is the subject: it
 * calls {@link #onEventRecorded} on every registered observer each time it
 * stores an event.
 *
 * <p>Notifications happen on the thread that records the event, so
 * implementations should return quickly. An observer that throws a
 * RuntimeException does not stop the event from being stored or the other
 * observers from being notified.
 */
@FunctionalInterface
public interface TelemetryObserver {

    /** Called after the event has been stored by the manager. */
    void onEventRecorded(TelemetryEvent event);
}
