package com.comp3607grp.telemetry.listener;

import bluej.extensions2.event.InvocationFinishedEvent;
import bluej.extensions2.event.InvocationFinishedListener;

import java.time.LocalDateTime;

import com.comp3607grp.telemetry.TelemetryEvent;
import com.comp3607grp.telemetry.TelemetryManager;

public class ExecutionListener implements InvocationFinishedListener {

    private final TelemetryManager manager;

    public ExecutionListener(TelemetryManager manager) {
        if (manager == null) {
            throw new IllegalArgumentException("TelemetryManager cannot be null");
        }

        this.manager = manager;
    }

    @Override
    public void invocationFinished(InvocationFinishedEvent event) {

        TelemetryEvent telemetryEvent =
            new TelemetryEvent("RUN", LocalDateTime.now());

        telemetryEvent.setClassName(event.getClassName());
        telemetryEvent.setMethodName(event.getMethodName());
        telemetryEvent.setObjectName(event.getObjectName());

        switch (event.getEventType()) {

            case NORMAL_EXIT:
                telemetryEvent.setStatus("SUCCESS");
                break;

            case EXCEPTION_EXIT:
                telemetryEvent.setStatus("FAILED");

                Object result = event.getResult();

                if (result != null) {
                    telemetryEvent.setExceptionMessage(
                        result.toString()
                    );
                }
                break;

            case TERMINATED_EXIT:
                telemetryEvent.setStatus("TERMINATED");
                break;

            case UNKNOWN_EXIT:
                telemetryEvent.setStatus("UNKNOWN");
                break;
        }

        manager.recordEvent(telemetryEvent);
    }
}
