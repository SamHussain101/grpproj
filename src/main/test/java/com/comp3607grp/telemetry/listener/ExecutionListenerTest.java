package com.comp3607grp.telemetry.listener;

import org.junit.jupiter.api.Test;

import com.comp3607grp.telemetry.TelemetryManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExecutionListenerTest {

    @Test void rejectsNullManager() {
        assertThrows(IllegalArgumentException.class, () -> new ExecutionListener(null));
    }

    @Test void acceptsManager() {
        assertNotNull(new ExecutionListener(new TelemetryManager()));
    }
}
