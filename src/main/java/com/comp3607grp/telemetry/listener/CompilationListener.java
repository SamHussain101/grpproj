package com.comp3607grp.telemetry.listener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.CompileListener;

import com.comp3607grp.telemetry.TelemetryEvent;
import com.comp3607grp.telemetry.TelemetryManager;

public class CompilationListener implements CompileListener {

    private final TelemetryManager manager;
    private final Supplier<LocalDateTime> clock;
    private TelemetryEvent currentCompile;
    private LocalDateTime previousStart;

    public CompilationListener(TelemetryManager manager) {
        this(manager, LocalDateTime::now);
    }

    CompilationListener(TelemetryManager manager, Supplier<LocalDateTime> clock) {
        if (manager == null) {
            throw new IllegalArgumentException("TelemetryManager cannot be null");
        }

        this.manager = manager;
        this.clock = clock;
    }

    @Override
    public synchronized void compileStarted(CompileEvent event) {
        if (!event.isUserGeneratedCompilation()) {
            currentCompile = null;
            return;
        }

        LocalDateTime now = clock.get();
        currentCompile = new TelemetryEvent("COMPILE", now);

        if (previousStart != null) {
            currentCompile.setIntervalSeconds(
                Math.max(0, Duration.between(previousStart, now).toMillis() / 1000.0)
            );
        }
        previousStart = now;
    }

    @Override
    public synchronized void compileError(CompileEvent event) {
        if (currentCompile == null) {
            return;
        }

        currentCompile.addError(
            event.getErrorMessage(),
            event.getErrorLineNumber()
        );
    }

    @Override
    public synchronized void compileWarning(CompileEvent event) {
    }

    @Override
    public synchronized void compileSucceeded(CompileEvent event) {
        finishCompilation("SUCCESS");
    }

    @Override
    public synchronized void compileFailed(CompileEvent event) {
        finishCompilation("FAILED");
    }

    private void finishCompilation(String status) {
        if (currentCompile != null) {
            currentCompile.setStatus(status);
            manager.recordEvent(currentCompile);
            currentCompile = null;
        }
    }
}
