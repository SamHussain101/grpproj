package com.comp3607grp.telemetry;

import bluej.extensions2.BPackage;
import bluej.extensions2.MenuGenerator;
import javafx.scene.control.Alert;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class TelemetryMenuGenerator extends MenuGenerator {

    private final TelemetryManager manager;
    private final ReportExporter exporter;

    public TelemetryMenuGenerator(TelemetryManager manager, ReportExporter exporter) {
        if (manager == null || exporter == null) {
            throw new IllegalArgumentException("Manager and exporter cannot be null");
        }

        this.manager = manager;
        this.exporter = exporter;
    }

    @Override
    public MenuItem getToolsMenuItem(BPackage aPackage) {

        MenuItem summaryItem = new MenuItem("Show Summary");
        summaryItem.setOnAction(event -> showSummary());

        MenuItem exportItem = new MenuItem("Export Report (JSON + PDF)");
        exportItem.setOnAction(event -> exportReport());

        Menu menu = new Menu("Telemetry");
        menu.getItems().addAll(summaryItem, exportItem);
        return menu;
    }

    private void showSummary() {

        String summary =
            "Total Events: " + manager.getEvents().size() + "\n"
            + "Total Compilations: " + manager.getTotalCompilations() + "\n"
            + "Successful Compilations: " + manager.getSuccessfulCompilations() + "\n"
            + "Failed Compilations: " + manager.getFailedCompilations() + "\n"
            + "Total Compilation Errors: " + manager.getTotalErrors() + "\n\n"

            + "Total Method Invocations: " + manager.getTotalRuns() + "\n"
            + "Successful Invocations: " + manager.getSuccessfulRuns() + "\n"
            + "Failed Invocations: " + manager.getFailedRuns() + "\n\n"

            + "Editor Activity Events: " + manager.getTotalEditorActivities() + "\n"
            + "Recorded Editor Activity Time: "
            + manager.getTotalEditorActivitySeconds() + " seconds";

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Telemetry Summary");
        alert.setHeaderText("Collected Telemetry Statistics");
        alert.setContentText(summary);
        alert.showAndWait();
    }

    private void exportReport() {

        Alert alert;
        try {
            List<Path> files = exporter.export(manager.getEvents());
            StringBuilder text = new StringBuilder("Saved:");
            for (Path file : files) {
                text.append("\n").append(file);
            }
            alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Report exported");
            alert.setContentText(text.toString());
        } catch (IOException | RuntimeException e) {
            alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Export failed");
            alert.setContentText(String.valueOf(e.getMessage()));
        }
        alert.setTitle("Telemetry Export");
        alert.showAndWait();
    }
}
