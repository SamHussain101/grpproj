package com.comp3607grp.telemetry;

import java.nio.file.Path;
import java.util.List;

import bluej.extensions2.BlueJ;
import bluej.extensions2.Extension;

import com.comp3607grp.telemetry.listener.*;
import com.comp3607grp.telemetry.report.*;

public class TelemetryExtension extends Extension {

    private BlueJ blueJ;

    private TelemetryManager manager;

    private CompilationListener compilationListener;
    private ExecutionListener executionListener;
    private ProjectLifecycleListener projectListener;
    private ClassLifecycleListener classListener;
    private TelemetryMenuGenerator menuGenerator;
    private ReportExporter exporter;

    @Override
    public boolean isCompatible() {
        return true;
    }

    @Override
    public void startup(BlueJ blueJ) {

        this.blueJ = blueJ;

        manager = new TelemetryManager();

        compilationListener = new CompilationListener(manager);
        executionListener = new ExecutionListener(manager);
        projectListener = new ProjectLifecycleListener(manager);
        classListener = new ClassLifecycleListener(manager);

        exporter = new ReportExporter(
                Path.of(System.getProperty("user.home"), "BlueJ-Telemetry"),
                List.of(new JsonExporter(), new PdfReportGenerator()));
        menuGenerator = new TelemetryMenuGenerator(manager, exporter);
        blueJ.setMenuGenerator(menuGenerator);

        blueJ.addCompileListener(compilationListener);
        blueJ.addInvocationFinishedListener(executionListener);
        blueJ.addPackageListener(projectListener);
        blueJ.addClassListener(classListener);

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

        if (manager != null && exporter != null && !manager.getEvents().isEmpty()) {
            try {
                List<Path> files = exporter.export(manager.getEvents());
                System.out.println("Telemetry report saved: " + files);
            } catch (Exception e) {
                System.out.println("Telemetry export failed: " + e);
            }
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
