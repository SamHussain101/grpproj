package com.comp3607grp.telemetry.listener;

import java.time.LocalDateTime;

import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.CompileListener;

import com.comp3607grp.telemetry.TelemetryEvent;
import com.comp3607grp.telemetry.TelemetryManager;

public class CompilationListener implements CompileListener {

    private final TelemetryManager manager;
    private TelemetryEvent currentCompile;

    public CompilationListener(TelemetryManager manager) {
        if (manager == null) {
            throw new IllegalArgumentException("TelemetryManager cannot be null");
        }

        this.manager = manager;
    }

    @Override
    public synchronized void compileStarted(CompileEvent event) {
        currentCompile = new TelemetryEvent("COMPILE", LocalDateTime.now());
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
