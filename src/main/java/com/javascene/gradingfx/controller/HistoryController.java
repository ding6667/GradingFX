package com.javascene.gradingfx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class HistoryController {

    @FXML private Button refreshBtn;
    @FXML private Button viewResultBtn;
    @FXML private Button reExportBtn;
    @FXML private Button deleteTaskBtn;

    @FXML private TableView<HistoryTask> historyTable;
    @FXML private TableColumn<HistoryTask, String> taskNameColumn;
    @FXML private TableColumn<HistoryTask, String> taskTimeColumn;
    @FXML private TableColumn<HistoryTask, String> studentCountColumn;
    @FXML private TableColumn<HistoryTask, String> taskStatusColumn;
    @FXML private TableColumn<HistoryTask, String> operationColumn;

    @FXML
    public void initialize() {
        // Setup columns
        taskNameColumn.setCellValueFactory(cellData -> cellData.getValue().taskNameProperty());
        taskTimeColumn.setCellValueFactory(cellData -> cellData.getValue().taskTimeProperty());
        studentCountColumn.setCellValueFactory(cellData -> cellData.getValue().studentCountProperty());
        taskStatusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        operationColumn.setCellValueFactory(cellData -> cellData.getValue().operationProperty());

        // Load mock data
        loadMockData();
    }

    private void loadMockData() {
        historyTable.getItems().addAll(
            new HistoryTask("Java作业_2024秋_计科1班", "2024-12-20 14:30", "45", "✅ 已完成", "👁 查看"),
            new HistoryTask("数据结构_期中_计科2班", "2024-12-18 09:15", "42", "✅ 已完成", "👁 查看"),
            new HistoryTask("Java作业_2024秋_软工1班", "2024-12-15 16:45", "38", "✅ 已完成", "👁 查看"),
            new HistoryTask("算法设计_实验3_计科1班", "2024-12-12 11:20", "44", "⚠️ 部分失败", "👁 查看")
        );
    }

    @FXML void handleRefresh() {
        System.out.println("Refreshing history...");
    }

    @FXML void handleViewResult() {
        System.out.println("Viewing result...");
    }

    @FXML void handleReExport() {
        System.out.println("Re-exporting...");
    }

    @FXML void handleDeleteTask() {
        System.out.println("Deleting task...");
    }

    // Simple model class for history
    public static class HistoryTask {
        private final javafx.beans.property.SimpleStringProperty taskName;
        private final javafx.beans.property.SimpleStringProperty taskTime;
        private final javafx.beans.property.SimpleStringProperty studentCount;
        private final javafx.beans.property.SimpleStringProperty status;
        private final javafx.beans.property.SimpleStringProperty operation;

        public HistoryTask(String taskName, String taskTime, String studentCount,
                          String status, String operation) {
            this.taskName = new javafx.beans.property.SimpleStringProperty(taskName);
            this.taskTime = new javafx.beans.property.SimpleStringProperty(taskTime);
            this.studentCount = new javafx.beans.property.SimpleStringProperty(studentCount);
            this.status = new javafx.beans.property.SimpleStringProperty(status);
            this.operation = new javafx.beans.property.SimpleStringProperty(operation);
        }

        public javafx.beans.property.SimpleStringProperty taskNameProperty() { return taskName; }
        public javafx.beans.property.SimpleStringProperty taskTimeProperty() { return taskTime; }
        public javafx.beans.property.SimpleStringProperty studentCountProperty() { return studentCount; }
        public javafx.beans.property.SimpleStringProperty statusProperty() { return status; }
        public javafx.beans.property.SimpleStringProperty operationProperty() { return operation; }
    }
}