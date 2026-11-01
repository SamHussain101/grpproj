

package com.comp3607grp.telemetry;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class EventBuffer {

    private final BlockingQueue<TelemetryEvent> queue =
            new LinkedBlockingQueue<>();

    public void addEvent(TelemetryEvent event) {

        if (event == null) {
            throw new IllegalArgumentException(
                    "Event cannot be null"
            );
        }

        queue.offer(event);
    }

    public TelemetryEvent getEvent()
            throws InterruptedException {

        return queue.take();
    }

    public TelemetryEvent pollEvent(long timeoutMillis)
            throws InterruptedException {

        return queue.poll(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }
}
