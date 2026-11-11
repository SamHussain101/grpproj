package com.comp3607grp.telemetry.listener;

import org.junit.jupiter.api.Test;

import com.comp3607grp.telemetry.TelemetryManager;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class EditorActivityListenerTest {

    @Test void rejectsNullManagerOrClass() {
        assertThrows(IllegalArgumentException.class, () -> new EditorActivityListener(null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new EditorActivityListener(new TelemetryManager(), null));
    }
}
