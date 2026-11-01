
package com.comp3607grp.telemetry;

import java.time.LocalDateTime;

import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.CompileListener;

public class CompilationListener implements CompileListener {

    private final EventBuffer buffer;
    private TelemetryEvent currentCompile;

    public CompilationListener(EventBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("EventBuffer cannot be null");
        }

        this.buffer = buffer;
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
            buffer.addEvent(currentCompile);
            currentCompile = null;
        }
    }
}
