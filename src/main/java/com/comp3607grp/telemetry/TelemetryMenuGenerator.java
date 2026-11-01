
package com.comp3607grp.telemetry;

import bluej.extensions2.BPackage;
import bluej.extensions2.MenuGenerator;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;

public class TelemetryMenuGenerator extends MenuGenerator {

    private final TelemetryManager manager;

    public TelemetryMenuGenerator(TelemetryManager manager) {
        if (manager == null) {
            throw new IllegalArgumentException("Manager cannot be null");
        }

        this.manager = manager;
    }

    @Override
    public MenuItem getToolsMenuItem(BPackage aPackage) {

        MenuItem menuItem = new MenuItem("Telemetry Summary");

        menuItem.setOnAction(event -> showSummary());

        return menuItem;
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
}
