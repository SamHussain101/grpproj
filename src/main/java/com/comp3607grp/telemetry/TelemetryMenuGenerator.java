package com.comp3607grp.telemetry;

import bluej.extensions2.BPackage;
import bluej.extensions2.MenuGenerator;
import javafx.scene.control.Alert;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.comp3607grp.telemetry.analytics.ReportAggregator;
import com.comp3607grp.telemetry.report.ReportExporter;

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

        // One snapshot of the events, so every figure comes from the same data.
        ReportAggregator stats = new ReportAggregator(manager.getEvents());

        String summary =
            "Total Events: " + stats.getEvents().size() + "\n"
            + "Total Compilations: " + stats.getTotalCompilations() + "\n"
            + "Successful Compilations: " + stats.getSuccessfulCompilations() + "\n"
            + "Failed Compilations: " + stats.getFailedCompilations() + "\n"
            + "Total Compilation Errors: " + stats.getTotalCompileErrors() + "\n\n"

            + "Total Method Invocations: " + stats.getTotalRuns() + "\n"
            + "Successful Invocations: " + stats.getSuccessfulRuns() + "\n"
            + "Failed Invocations: " + stats.getFailedRuns() + "\n\n"

            + "Editor Activity Events: " + stats.getTotalEdits() + "\n"
            + "Recorded Editor Activity Time: "
            + stats.getActiveTimeSeconds() + " seconds";

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
