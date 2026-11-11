package com.comp3607grp.telemetry.listener;

import org.junit.jupiter.api.Test;

import com.comp3607grp.telemetry.TelemetryManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ClassLifecycleListenerTest {

    @Test void rejectsNullManager() {
        assertThrows(IllegalArgumentException.class, () -> new ClassLifecycleListener(null));
    }

    @Test void acceptsManager() {
        assertNotNull(new ClassLifecycleListener(new TelemetryManager()));
    }
}
