package com.javascene.gradingfx.controller;

import com.javascene.gradingfx.enmu.HistoryStatus;
import com.javascene.gradingfx.model.HistoryTask;
import com.javascene.gradingfx.service.HistoryService;
import com.javascene.gradingfx.service.Impl.HistoryServiceImpl;
import com.javascene.gradingfx.service.ReviewService;
import com.javascene.gradingfx.service.Impl.ReviewServiceImpl;
import com.javascene.gradingfx.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;

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
    @FXML private TableColumn<HistoryTask, Void> operationColumn;

    private final HistoryService historyService = new HistoryServiceImpl();
    private final ReviewService reviewService = new ReviewServiceImpl();

    /** 选中的任务ID，供 MainController 回传使用 */
    private String selectedTaskId;
    /** 查看结果回调，关闭弹窗后由 MainController 读取 selectedTaskId */
    private Consumer<String> onResultSelected;

    public void setOnResultSelected(Consumer<String> callback) {
        this.onResultSelected = callback;
    }

    public String getSelectedTaskId() {
        return selectedTaskId;
    }

    @FXML
    public void initialize() {
        setupColumns();
        setupOperationColumn();
        loadTasks();

        historyTable.setPlaceholder(new Label("暂无历史批阅记录"));
        viewResultBtn.setDisable(true);
        reExportBtn.setDisable(true);
        deleteTaskBtn.setDisable(true);
        historyTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    boolean hasSelection = newVal != null;
                    viewResultBtn.setDisable(!hasSelection);
                    reExportBtn.setDisable(!hasSelection);
                    deleteTaskBtn.setDisable(!hasSelection);
                });
    }

    private void setupColumns() {
        taskNameColumn.setCellValueFactory(cd -> cd.getValue().taskNameProperty());
        taskTimeColumn.setCellValueFactory(cd -> cd.getValue().createTimeProperty());
        studentCountColumn.setCellValueFactory(cd -> {
            HistoryTask t = cd.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                    t.getGradedStudents() + "/" + t.getTotalStudents());
        });
        taskStatusColumn.setCellValueFactory(cd -> {
            HistoryStatus s = cd.getValue().getStatus();
            return new javafx.beans.property.SimpleStringProperty(s != null ? s.getDisplayName() : "");
        });
        // 状态列颜色渲染
        taskStatusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                if (empty || item == null) {
                    setStyle("");
                    return;
                }
                HistoryTask task = getTableRow() != null ? getTableRow().getItem() : null;
                if (task != null && task.getStatus() != null) {
                    setStyle("-fx-text-fill: " + switch (task.getStatus()) {
                        case COMPLETED -> "#10B981";
                        case GRADING -> "#F59E0B";
                        case FAILED, PARTIAL_FAILURE -> "#EF4444";
                        default -> "#64748B";
                    } + "; -fx-font-weight: bold;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void setupOperationColumn() {
        operationColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("查看");
            private final HBox box = new HBox(6, viewBtn);
            {
                viewBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4F46E5;"
                        + " -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 2 8;");
                viewBtn.setOnAction(e -> {
                    HistoryTask task = getTableRow() != null ? getTableRow().getItem() : null;
                    if (task != null) {
                        selectedTaskId = task.getTaskId();
                        if (onResultSelected != null) {
                            onResultSelected.accept(selectedTaskId);
                        }
                        closeWindow();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void loadTasks() {
        List<HistoryTask> tasks = historyService.loadAllTasks();
        historyTable.setItems(FXCollections.observableArrayList(tasks));
    }

    @FXML
    void handleRefresh() {
        loadTasks();
    }

    @FXML
    void handleViewResult() {
        HistoryTask selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("请先选择一个任务");
            return;
        }
        selectedTaskId = selected.getTaskId();
        if (onResultSelected != null) {
            onResultSelected.accept(selectedTaskId);
        }
        closeWindow();
    }

    @FXML
    void handleReExport() {
        HistoryTask selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("请先选择一个任务");
            return;
        }

        Alert choice = AlertUtil.createAlert(Alert.AlertType.CONFIRMATION,
                "选择导出格式", "请选择导出格式", "");
        ButtonType excelBtn = new ButtonType("Excel (.xlsx)");
        ButtonType wordBtn = new ButtonType("Word (.docx)");
        ButtonType cancelBtn = ButtonType.CANCEL;
        choice.getButtonTypes().setAll(excelBtn, wordBtn, cancelBtn);
        choice.showAndWait().ifPresent(type -> {
            if (type == cancelBtn) return;

            String taskId = selected.getTaskId();

            Task<String> exportTask = new Task<>() {
                @Override
                protected String call() {
                    return type == excelBtn
                            ? reviewService.exportExcel(taskId)
                            : reviewService.exportWord(taskId);
                }
            };

            exportTask.setOnSucceeded(e -> {
                String path = exportTask.getValue();
                if (path != null) {
                    AlertUtil.showInfo("导出成功", "文件路径: " + path);
                } else {
                    AlertUtil.showError("导出失败，未找到任务数据");
                }
            });

            exportTask.setOnFailed(e ->
                    AlertUtil.showError("导出失败: " + exportTask.getException().getMessage()));

            Thread thread = new Thread(exportTask);
            thread.setDaemon(true);
            thread.start();
        });
    }

    @FXML
    void handleDeleteTask() {
        HistoryTask selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("请先选择一个任务");
            return;
        }

        boolean confirmed = AlertUtil.showConfirm(
                "确认删除",
                "确定要删除任务「" + selected.getTaskName() + "」及其关联数据吗？");
        if (!confirmed) return;

        List<HistoryTask> remaining = historyService.deleteTask(selected.getTaskId());
        historyTable.setItems(FXCollections.observableArrayList(remaining));
        AlertUtil.showInfo("删除成功", "任务 " + selected.getTaskName() + " 已删除");
    }

    private void closeWindow() {
        if (historyTable.getScene() != null && historyTable.getScene().getWindow() != null) {
            Stage stage = (Stage) historyTable.getScene().getWindow();
            stage.close();
        }
    }
}
