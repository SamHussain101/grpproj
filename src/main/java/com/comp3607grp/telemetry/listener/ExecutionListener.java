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
        manager.recordEvent(toTelemetryEvent(
            event.getEventType(),
            event.getClassName(),
            event.getMethodName(),
            event.getObjectName(),
            event.getResult(),
            LocalDateTime.now()
        ));
    }

    static TelemetryEvent toTelemetryEvent(InvocationFinishedEvent.EventType type,
            String className, String methodName, String objectName,
            Object result, LocalDateTime when) {

        TelemetryEvent telemetryEvent = new TelemetryEvent("RUN", when);

        telemetryEvent.setClassName(className);
        telemetryEvent.setMethodName(methodName);
        telemetryEvent.setObjectName(objectName);

        switch (type) {

            case NORMAL_EXIT:
                telemetryEvent.setStatus("SUCCESS");
                break;

            case EXCEPTION_EXIT:
                telemetryEvent.setStatus("FAILED");
                if (result != null) {
                    telemetryEvent.setExceptionMessage(result.toString());
                }
                break;

            case TERMINATED_EXIT:
                telemetryEvent.setStatus("TERMINATED");
                break;

            default:
                telemetryEvent.setStatus("UNKNOWN");
                break;
        }

        return telemetryEvent;
    }
}
