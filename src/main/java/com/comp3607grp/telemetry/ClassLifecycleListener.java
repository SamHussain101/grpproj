
package com.comp3607grp.telemetry;

import java.time.LocalDateTime;
import java.util.IdentityHashMap;
import java.util.Map;

import bluej.extensions2.BClass;
import bluej.extensions2.BPackage;
import bluej.extensions2.BProject;
import bluej.extensions2.editor.JavaEditor;
import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.ClassListener;

public class ClassLifecycleListener implements ClassListener {

    private final EventBuffer buffer;

    // Keep track of editors that already have a listener attached.
    private final Map<BClass, EditorActivityListener> editorListeners =
            new IdentityHashMap<>();

    private final Map<BClass, JavaEditor> attachedEditors =
            new IdentityHashMap<>();

    public ClassLifecycleListener(EventBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("EventBuffer cannot be null");
        }
        this.buffer = buffer;
    }

    @Override
    public synchronized void classStateChanged(ClassEvent event) {
        recordClassEvent(event, "CLASS_STATE_CHANGED");
        attachEditorListener(event.getBClass());
    }

    @Override
    public synchronized void classNameChanged(ClassEvent event) {
        recordClassEvent(event, "CLASS_RENAMED");
        attachEditorListener(event.getBClass());
    }

    @Override
    public synchronized void classRemoved(ClassEvent event) {
        recordClassEvent(event, "CLASS_REMOVED");
        detachEditorListener(event.getBClass());
    }

    private void recordClassEvent(ClassEvent event, String eventType) {

        TelemetryEvent telemetryEvent =
                new TelemetryEvent(eventType, LocalDateTime.now());

        BClass blueJClass = event.getBClass();

        if (blueJClass != null) {
            telemetryEvent.setClassName(blueJClass.getName());
        }

        try {
            BPackage blueJPackage = event.getPackage();
            BProject project = blueJPackage.getProject();

            telemetryEvent.setProjectName(project.getName());
            telemetryEvent.setStatus("SUCCESS");

        } catch (Exception e) {
            telemetryEvent.setStatus("UNKNOWN");
        }

        buffer.addEvent(telemetryEvent);
    }

    private synchronized void attachEditorListener(BClass blueJClass) {

        if (blueJClass == null || editorListeners.containsKey(blueJClass)) {
            return;
        }

        try {
            JavaEditor editor = blueJClass.getJavaEditorIfOpen();

            if (editor == null) {
                return;
            }

            EditorActivityListener listener =
                    new EditorActivityListener(buffer, blueJClass);

            editor.addDocumentListener(listener);

            editorListeners.put(blueJClass, listener);
            attachedEditors.put(blueJClass, editor);

        } catch (Exception e) {
            System.err.println(
                    "Could not attach editor telemetry listener: "
                    + e.getMessage()
            );
        }
    }

    private synchronized void detachEditorListener(BClass blueJClass) {

        if (blueJClass == null) {
            return;
        }

        EditorActivityListener listener = editorListeners.remove(blueJClass);
        JavaEditor editor = attachedEditors.remove(blueJClass);

        if (listener != null && editor != null) {
            editor.removeDocumentListener(listener);
        }
    }
}