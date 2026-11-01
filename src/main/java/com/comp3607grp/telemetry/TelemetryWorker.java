

package com.comp3607grp.telemetry;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TelemetryWorker {

    private final EventBuffer buffer;
    private final TelemetryManager manager;

    private final ExecutorService executor;

    private final AtomicBoolean running =
            new AtomicBoolean(false);

    public TelemetryWorker(
            EventBuffer buffer,
            TelemetryManager manager) {

        if (buffer == null || manager == null) {
            throw new IllegalArgumentException(
                    "Buffer and manager cannot be null"
            );
        }

        this.buffer = buffer;
        this.manager = manager;

        this.executor =
                Executors.newSingleThreadExecutor();
    }

    public void start() {

        if (running.compareAndSet(false, true)) {
            executor.submit(this::processEvents);
        }
    }

    private void processEvents() {

        while (running.get() || !buffer.isEmpty()) {

            try {
                TelemetryEvent event =
                        buffer.pollEvent(500);

                if (event != null) {
                    manager.recordEvent(event);
                }

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void stop() {

        running.set(false);
        executor.shutdown();

        try {
            if (!executor.awaitTermination(
                    5, TimeUnit.SECONDS)) {

                executor.shutdownNow();
            }

        } catch (InterruptedException e) {

            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
