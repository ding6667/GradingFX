package com.javascene.gradingfx.controller;

import com.javascene.gradingfx.enmu.HistoryStatus;
import com.javascene.gradingfx.model.HistoryTask;
import com.javascene.gradingfx.service.Impl.HistoryServiceImpl;
import com.javascene.gradingfx.service.HistoryService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

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
    @FXML private TableColumn<HistoryTask, ?> operationColumn;

    private final HistoryService historyService = new HistoryServiceImpl();

    @FXML
    public void initialize() {
        taskNameColumn.setCellValueFactory(cellData -> cellData.getValue().taskNameProperty());
        taskTimeColumn.setCellValueFactory(cellData -> cellData.getValue().createTimeProperty());
        studentCountColumn.setCellValueFactory(cellData -> {
            HistoryTask t = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(t.getGradedStudents() + "/" + t.getTotalStudents());
        });
        taskStatusColumn.setCellValueFactory(cellData -> {
            HistoryStatus s = cellData.getValue().getStatus();
            return new javafx.beans.property.SimpleStringProperty(s != null ? s.getDisplayName() : "");
        });

        loadTasks();
    }

    private void loadTasks() {
        List<HistoryTask> tasks = historyService.loadAllTasks();
        historyTable.setItems(FXCollections.observableArrayList(tasks));
    }

    @FXML void handleRefresh() {
        loadTasks();
    }

    @FXML void handleViewResult() {
        HistoryTask selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            System.out.println("查看结果: " + selected.getTaskId());
        }
    }

    @FXML void handleReExport() {
        HistoryTask selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            System.out.println("重新导出: " + selected.getTaskId());
        }
    }

    @FXML void handleDeleteTask() {
        HistoryTask selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            System.out.println("删除任务: " + selected.getTaskId());
        }
    }
}
