
package com.comp3607grp.telemetry;

import bluej.extensions2.BlueJ;
import bluej.extensions2.Extension;

public class TelemetryExtension extends Extension {

    private BlueJ blueJ;

    private EventBuffer buffer;
    private TelemetryManager manager;
    private TelemetryWorker worker;

    private CompilationListener compilationListener;
    private ExecutionListener executionListener;
    private ProjectLifecycleListener projectListener;
    private ClassLifecycleListener classListener;
    private TelemetryMenuGenerator menuGenerator;

    @Override
    public boolean isCompatible() {
        return true;
    }

    @Override
    public void startup(BlueJ blueJ) {

        this.blueJ = blueJ;

        buffer = new EventBuffer();
        manager = new TelemetryManager();
        worker = new TelemetryWorker(buffer, manager);

        compilationListener = new CompilationListener(buffer);
        executionListener = new ExecutionListener(buffer);
        projectListener = new ProjectLifecycleListener(buffer);
        classListener = new ClassLifecycleListener(buffer);

        menuGenerator = new TelemetryMenuGenerator(manager);
        blueJ.setMenuGenerator(menuGenerator);

        blueJ.addCompileListener(compilationListener);
        blueJ.addInvocationFinishedListener(executionListener);
        blueJ.addPackageListener(projectListener);
        blueJ.addClassListener(classListener);

        worker.start();

        System.out.println("Telemetry Extension Started");
    }

    @Override
    public void terminate() {

        if (blueJ != null) {
            blueJ.removeCompileListener(compilationListener);
            blueJ.removeInvocationFinishedListener(executionListener);
            blueJ.removePackageListener(projectListener);
            blueJ.removeClassListener(classListener);
        }

        if (worker != null) {
            worker.stop();
        }

        System.out.println("Telemetry Extension Terminated");
    }

    @Override
    public String getName() {
        return "Telemetry";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Collects BlueJ project, class, compilation and execution telemetry.";
    }
}
