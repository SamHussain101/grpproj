
package com.comp3607grp.telemetry;

import bluej.extensions2.event.InvocationFinishedEvent;
import bluej.extensions2.event.InvocationFinishedListener;

import java.time.LocalDateTime;

public class ExecutionListener implements InvocationFinishedListener {

    private final EventBuffer buffer;

    public ExecutionListener(EventBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("EventBuffer cannot be null");
        }

        this.buffer = buffer;
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

        buffer.addEvent(telemetryEvent);
    }
}
