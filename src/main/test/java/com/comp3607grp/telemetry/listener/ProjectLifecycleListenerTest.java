package com.comp3607grp.telemetry.listener;

import org.junit.jupiter.api.Test;

import com.comp3607grp.telemetry.TelemetryManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProjectLifecycleListenerTest {

    @Test void rejectsNullManager() {
        assertThrows(IllegalArgumentException.class, () -> new ProjectLifecycleListener(null));
    }

    @Test void acceptsManager() {
        assertNotNull(new ProjectLifecycleListener(new TelemetryManager()));
    }
}
