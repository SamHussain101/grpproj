package com.comp3607grp.telemetry.listener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import bluej.extensions2.BClass;
import bluej.extensions2.BMethod;
import bluej.extensions2.BPackage;
import bluej.extensions2.BProject;
import bluej.extensions2.editor.JavaEditor;
import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.ClassListener;

import com.comp3607grp.telemetry.TelemetryEvent;
import com.comp3607grp.telemetry.TelemetryManager;

public class ClassLifecycleListener implements ClassListener {

    private final TelemetryManager manager;

    //track the editors that have listners
    private final Map<BClass, EditorActivityListener> editorListeners =
            new IdentityHashMap<>();

    private final Map<BClass, JavaEditor> attachedEditors =
            new IdentityHashMap<>();

    private final Map<String, Set<String>> knownMethods = new HashMap<>();

    public ClassLifecycleListener(TelemetryManager manager) {
        if (manager == null) {
            throw new IllegalArgumentException("TelemetryManager cannot be null");
        }
        this.manager = manager;
    }

    @Override
    public synchronized void classStateChanged(ClassEvent event) {
        recordClassEvent(event, "CLASS_STATE_CHANGED");
        attachEditorListener(event.getBClass());
        if (event.isClassCompiled()) {
            recordMethodChanges(event);
        }
    }

    @Override
    public synchronized void classNameChanged(ClassEvent event) {
        recordClassEvent(event, "CLASS_RENAMED");
        attachEditorListener(event.getBClass());
        if (event.getOldName() != null && event.getBClass() != null) {
            Set<String> methods = knownMethods.remove(event.getOldName());
            if (methods != null) {
                knownMethods.put(event.getBClass().getName(), methods);
            }
        }
    }

    @Override
    public synchronized void classRemoved(ClassEvent event) {
        recordClassEvent(event, "CLASS_REMOVED");
        detachEditorListener(event.getBClass());
        if (event.getBClass() != null) {
            knownMethods.remove(event.getBClass().getName());
        }
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

        manager.recordEvent(telemetryEvent);
    }

    private void recordMethodChanges(ClassEvent event) {

        BClass blueJClass = event.getBClass();
        if (blueJClass == null) {
            return;
        }

        Set<String> current = new TreeSet<>();
        try {
            for (BMethod method : blueJClass.getDeclaredMethods()) {
                current.add(signature(method));
            }
        } catch (Exception e) {
            return;
        }

        String className = blueJClass.getName();
        Set<String> previous = knownMethods.getOrDefault(className, Set.of());

        for (String method : current) {
            if (!previous.contains(method)) {
                recordMethodEvent(event, "METHOD_ADDED", className, method);
            }
        }
        for (String method : previous) {
            if (!current.contains(method)) {
                recordMethodEvent(event, "METHOD_REMOVED", className, method);
            }
        }

        knownMethods.put(className, current);
    }

    private void recordMethodEvent(ClassEvent event, String eventType, String className, String method) {

        TelemetryEvent telemetryEvent = new TelemetryEvent(eventType, LocalDateTime.now());
        telemetryEvent.setClassName(className);
        telemetryEvent.setMethodName(method);
        telemetryEvent.setStatus("SUCCESS");

        try {
            telemetryEvent.setProjectName(event.getPackage().getProject().getName());
        } catch (Exception e) {
            telemetryEvent.setStatus("UNKNOWN");
        }

        manager.recordEvent(telemetryEvent);
    }

    private static String signature(BMethod method) {

        StringBuilder sb = new StringBuilder(method.getName()).append('(');
        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(params[i].getSimpleName());
        }
        return sb.append(')').toString();
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
                    new EditorActivityListener(manager, blueJClass);

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