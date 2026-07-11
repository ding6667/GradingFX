package com.javascene.gradingfx.controller;

import com.javascene.gradingfx.config.property.AppConfig;
import com.javascene.gradingfx.constant.ErrorMessageConstant;
import com.javascene.gradingfx.enmu.ReviewStatus;
import com.javascene.gradingfx.model.GradingTask;
import com.javascene.gradingfx.model.HistoryTask;
import com.javascene.gradingfx.model.StudentResult;
import com.javascene.gradingfx.model.StudentResultProperty;
import com.javascene.gradingfx.service.HistoryService;
import com.javascene.gradingfx.service.Impl.HistoryServiceImpl;
import com.javascene.gradingfx.service.Impl.ReviewServiceImpl;
import com.javascene.gradingfx.service.Impl.StandardServiceImpl;
import com.javascene.gradingfx.service.ReviewService;
import com.javascene.gradingfx.service.StandardService;
import com.javascene.gradingfx.util.AlertUtil;
import com.javascene.gradingfx.util.ConfigLoader;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainController {

    @FXML private StackPane uploadArea;
    @FXML private VBox selectedFilesArea;
    @FXML private ListView<File> selectedFilesList;
    @FXML private Button clearFilesBtn;
    @FXML private TreeView<String> studentTree;
    @FXML private Label totalLabel;
    @FXML private Label completedLabel;
    @FXML private Label failedLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private Button startBtn;
    @FXML private Button pauseBtn;
    @FXML private Button resumeBtn;
    @FXML private Button retryBtn;
    @FXML private Button detailBtn;
    @FXML private Button exportExcelBtn;
    @FXML private Button exportWordBtn;
    @FXML private Button exportConfigBtn;
    @FXML private Button standardBtn;
    @FXML private Button historyBtn;

    private final ObservableList<String> selectedFiles = FXCollections.observableArrayList();

    @FXML private TableView<StudentResultProperty> resultTable;
    @FXML private TableColumn<StudentResultProperty, String> studentIdColumn;
    @FXML private TableColumn<StudentResultProperty, String> studentNameColumn;
    @FXML private TableColumn<StudentResultProperty, String> rawScoreColumn;
    @FXML private TableColumn<StudentResultProperty, String> aiCommentColumn;
    @FXML private TableColumn<StudentResultProperty, String> teacherScoreColumn;
    @FXML private TableColumn<StudentResultProperty, String> teacherCommentColumn;
    @FXML private TableColumn<StudentResultProperty, String> teacherNoteColumn;

    private final ObservableList<StudentResultProperty> studentData = FXCollections.observableArrayList();
    private final ObservableList<File> selectedFileObjects = FXCollections.observableArrayList();
    private final HistoryService historyService = new HistoryServiceImpl();
    private final ReviewService reviewService = new ReviewServiceImpl();
    private final StandardService standardService = new StandardServiceImpl();

    private final AppConfig appConfig = ConfigLoader.getConfig();

    /** 当前批阅任务ID */
    private String currentTaskId = null;

    /** 当前是否暂停 */
    private boolean isPaused = false;


    @FXML
    public void initialize() {
        studentIdColumn.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        studentNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        rawScoreColumn.setCellValueFactory(cellData -> cellData.getValue().rawScoreProperty());
        aiCommentColumn.setCellValueFactory(cellData -> cellData.getValue().aiCommentProperty());
        teacherScoreColumn.setCellValueFactory(cellData -> cellData.getValue().teacherScoreProperty());
        teacherCommentColumn.setCellValueFactory(cellData -> cellData.getValue().teacherCommentProperty());
        teacherNoteColumn.setCellValueFactory(cellData -> cellData.getValue().teacherNoteProperty());

        // 初始化已选文件列表
        selectedFilesList.setItems(selectedFileObjects);
        selectedFilesList.setCellFactory(lv -> new javafx.scene.control.ListCell<File>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (" + formatFileSize(item.length()) + ")");
                }
            }
        });

        loadData();

        // 初始状态：无文件，所有控制按钮禁用
        updateButtonStates(false, false);
    }

    private void loadData() {
        // 取最近一次的批阅任务ID
        List<GradingTask> gradingTasks = new ArrayList<>(reviewService.loadAllTasks());
        gradingTasks.sort(Comparator.comparing(GradingTask::getCreateTime).reversed());
        if (!gradingTasks.isEmpty()) {
            String taskId = gradingTasks.get(0).getId();
            studentData.addAll(reviewService.loadTask(taskId));
        }
        resultTable.setItems(studentData);
        totalLabel.setText(String.valueOf(studentData.size()));

        long completed = studentData.stream().filter(r -> r.getStatus() == ReviewStatus.APPROVED).count();
        long failed = studentData.stream().filter(r -> r.getStatus() == ReviewStatus.FAILED).count();
        completedLabel.setText(String.valueOf(completed));
        failedLabel.setText(String.valueOf(failed));

        setupStudentTree();
    }

    public void loadResults(String taskId) {
        List<StudentResultProperty> results = historyService.loadStudentResults(taskId);
        studentData.setAll(results);
        resultTable.setItems(studentData);
        totalLabel.setText(String.valueOf(studentData.size()));
        long completed = results.stream().filter(r -> r.getStatus() == ReviewStatus.APPROVED).count();
        long failed = results.stream().filter(r -> r.getStatus() == ReviewStatus.FAILED).count();
        completedLabel.setText(String.valueOf(completed));
        failedLabel.setText(String.valueOf(failed));
    }

    private void setupStudentTree() {
        TreeItem<String> root = new TreeItem<>("学生列表");
        root.setExpanded(true);

        long pending = studentData.stream().filter(r -> r.getStatus() == ReviewStatus.PENDING).count();
        long completed = studentData.stream().filter(r -> r.getStatus() == ReviewStatus.APPROVED).count();
        long failed = studentData.stream().filter(r -> r.getStatus() == ReviewStatus.FAILED).count();

        TreeItem<String> pendingItem = new TreeItem<>(" 待批阅 (" + pending + ")");
        TreeItem<String> completedItem = new TreeItem<>(" 已完成 (" + completed + ")");
        TreeItem<String> failedItem = new TreeItem<>(" 失败 (" + failed + ")");

        for (StudentResultProperty r : studentData) {
            String label = r.getName() + " - " + r.getId();
            switch (r.getStatus()) {
                case PENDING -> pendingItem.getChildren().add(new TreeItem<>(label));
                case APPROVED -> completedItem.getChildren().add(new TreeItem<>(label));
                case FAILED -> failedItem.getChildren().add(new TreeItem<>(label));
            }
        }

        root.getChildren().addAll(pendingItem, completedItem, failedItem);
        studentTree.setRoot(root);
        studentTree.setShowRoot(false);
    }

    @FXML void handleOpenExportConfig() {
        showView("export-config-view.fxml", "导出路径配置", o -> {});
    }

    @FXML void handleOpenStandard() {
        showView("standard-view.fxml", "设置评分标准",o -> {});
    }

    @FXML void handleOpenHistory() {
        showView("history-view.fxml", "查看历史记录", controller -> {
            if (controller instanceof HistoryController hc) {
                hc.setOnResultSelected(taskId -> {
                    // showView 关闭后在 refresh 之后执行
                    javafx.application.Platform.runLater(() -> loadResults(taskId));
                });
            }
        });
    }

    @FXML void handleUploadClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择作业压缩包");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("ZIP 压缩包", "*.zip"),
                new FileChooser.ExtensionFilter("所有文件", "*.*")
        );
        File file = fileChooser.showOpenDialog(uploadArea.getScene().getWindow());
        if (file != null) {
            handleSelectedFile(file);
        }
    }

    @FXML void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    @FXML void handleDragEntered(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            uploadArea.setStyle("-fx-border-color: #4F46E5; -fx-border-width: 2; -fx-border-style: dashed; -fx-background-color: #EEF2FF;");
        }
        event.consume();
    }

    @FXML void handleDragExited(DragEvent event) {
        uploadArea.setStyle("");
        event.consume();
    }

    @FXML void handleDrop(DragEvent event) {
        uploadArea.setStyle("");
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            for (File file : db.getFiles()) {
                if (file.getName().toLowerCase().endsWith(".zip")) {
                    handleSelectedFile(file);
                    break;
                }
            }
        }
        event.setDropCompleted(true);
        event.consume();
    }

    @FXML void handleClearFiles() {
        selectedFileObjects.clear();
        updateSelectedFilesArea();
        updateButtonStates(reviewService.isReviewRunning(), isPaused);
    }

    private void handleSelectedFile(File file) {
        if (!selectedFileObjects.contains(file)) {
            selectedFileObjects.add(file);
            updateSelectedFilesArea();
            updateButtonStates(reviewService.isReviewRunning(), isPaused);
            System.out.println("已选择文件: " + file.getAbsolutePath());
        }
    }

    /**
     * 更新已选中文件区域的显示状态
     */
    private void updateSelectedFilesArea() {
        boolean hasFiles = !selectedFileObjects.isEmpty();
        selectedFilesArea.setVisible(hasFiles);
        selectedFilesArea.setManaged(hasFiles);
    }

    /**
     * 获取已选中的文件列表
     */
    public List<File> getSelectedFiles() {
        return new ArrayList<>(selectedFileObjects);
    }

    /**
     * 清除已选中的文件
     */
    public void clearSelectedFiles() {
        selectedFileObjects.clear();
        updateSelectedFilesArea();
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
    }

    @FXML void handleStart() {
        if (selectedFileObjects.isEmpty()) {
            AlertUtil.showError(ErrorMessageConstant.FILE_NOT_SELECTED);
            return;
        }
        if (reviewService.isReviewRunning()) {
            AlertUtil.showError(ErrorMessageConstant.REVIEW_RUNNING);
            return;
        }

        String zipPath = selectedFileObjects.get(0).getAbsolutePath();
        String rubric = standardService.getCurrentStandard();

        // 清空表格数据，准备新批阅
        studentData.clear();
        progressBar.setProgress(0);
        progressLabel.setText("准备中...");

        isPaused = false;
        updateButtonStates(true, false);

        // 启动批阅（service 内部创建调度线程，非阻塞）
        try {
            currentTaskId = reviewService.startBatchReviewFromZip(zipPath, rubric, createProgressCallback());
            if (currentTaskId == null) {
                updateButtonStates(false, false);
            }
        } catch (Exception e) {
            log.error("启动批阅失败: {}", e.getMessage(), e);
            progressLabel.setText(ErrorMessageConstant.REVIEW_ERROR_PREFIX + e.getMessage());
            updateButtonStates(false, false);
            AlertUtil.showError(ErrorMessageConstant.REVIEW_ERROR_PREFIX + e.getMessage());
        }
    }

    @FXML void handlePause() {
        reviewService.pauseReview();
        isPaused = true;
        updateButtonStates(true, true);
    }

    @FXML void handleResume() {
        reviewService.resumeReview();
        isPaused = false;
        updateButtonStates(true, false);
    }

    @FXML void handleRetry() {
        if (currentTaskId == null) {
            AlertUtil.showError(ErrorMessageConstant.NO_TASK_TO_RETRY);
            return;
        }
        if (reviewService.isReviewRunning()) {
            AlertUtil.showError(ErrorMessageConstant.REVIEW_RUNNING_CANNOT_RETRY);
            return;
        }

        String rubric = standardService.getCurrentStandard();
        isPaused = false;
        updateButtonStates(true, false);
        reviewService.retryFailed(currentTaskId, rubric, createProgressCallback());
    }

    /**
     * 创建批阅进度回调，通过 Platform.runLater 更新 UI
     */
    private ReviewService.ReviewProgressCallback createProgressCallback() {
        return new ReviewService.ReviewProgressCallback() {
            @Override
            public void onBatchCompleted(int completed, int failed, int total, List<StudentResult> batchResults) {
                Platform.runLater(() -> {
                    // 更新进度条
                    double progress = (double) (completed + failed) / total;
                    progressBar.setProgress(progress);
                    progressLabel.setText(String.format("已完成 %d / %d （成功 %d，失败 %d）",
                            completed + failed, total, completed, failed));

                    // 更新表格数据
                    for (StudentResult sr : batchResults) {
                        updateStudentResultProperty(sr);
                    }

                    // 更新统计标签
                    totalLabel.setText(String.valueOf(total));
                    completedLabel.setText(String.valueOf(completed));
                    failedLabel.setText(String.valueOf(failed));

                    // 更新树
                    setupStudentTree();
                });
            }

            @Override
            public void onReviewFinished(List<StudentResult> allResults) {
                Platform.runLater(() -> {
                    progressBar.setProgress(1.0);
                    progressLabel.setText("批阅完成");
                    isPaused = false;
                    updateButtonStates(false, false);

                    // 刷新整个表格
                    studentData.clear();
                    for (StudentResult sr : allResults) {
                        studentData.add(toProperty(sr));
                    }
                    resultTable.setItems(studentData);
                    setupStudentTree();
                });
            }

            @Override
            public void onReviewError(String error) {
                Platform.runLater(() -> {
                    progressLabel.setText(ErrorMessageConstant.REVIEW_ERROR_PREFIX + error);
                    isPaused = false;
                    updateButtonStates(false, false);
                    AlertUtil.showError(ErrorMessageConstant.REVIEW_ERROR_PREFIX + error);
                    refresh();
                });
            }
        };
    }

    /**
     * 将单个 StudentResult 更新到表格的 ObservableList 中
     */
    private void updateStudentResultProperty(StudentResult sr) {
        for (StudentResultProperty prop : studentData) {
            if (prop.getId() != null && prop.getId().equals(sr.getStudentId())) {
                prop.setRawScore(sr.getRawScore());
                prop.setAiComment(sr.getAiComment());
                prop.setErrorMessage(sr.getErrorMessage());
                try {
                    prop.setStatus(ReviewStatus.valueOf(sr.getStatus()));
                } catch (IllegalArgumentException e) {
                    prop.setStatus(ReviewStatus.FAILED);
                }
                return;
            }
        }
        // 未找到则新增
        studentData.add(toProperty(sr));
    }

    /**
     * StudentResult → StudentResultProperty 转换
     */
    private StudentResultProperty toProperty(StudentResult sr) {
        return new StudentResultProperty(
                sr.getTaskId(),
                sr.getStudentId(),
                sr.getStudentName(),
                sr.getRawScore(),
                sr.getAiComment(),
                sr.getTeacherScore(),
                sr.getTeacherComment(),
                sr.getTeacherNote(),
                sr.getStatus(),
                sr.getErrorMessage()
        );
    }

    /**
     * 更新按钮状态
     * @param running 是否正在批阅
     * @param paused 是否暂停
     */
    private void updateButtonStates(boolean running, boolean paused) {
        boolean hasFile = !selectedFileObjects.isEmpty();

        // 未上传文件时所有控制按钮禁用
        startBtn.setDisable(!hasFile || running);
        pauseBtn.setDisable(!hasFile || !running || paused);
        resumeBtn.setDisable(!hasFile || !paused);
        retryBtn.setDisable(!hasFile || running || currentTaskId == null);
    }

    @FXML void handleViewDetail() {
        StudentResultProperty selected = resultTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showError(ErrorMessageConstant.FILE_NOT_SELECTED);
            return;
        }
        showView("result-detail-view.fxml", "查看详情", controller -> {
            ((ResultDetailController) controller).setStudentResult(selected, currentTaskId);
        });
    }

    @FXML void handleExportExcel() {
        if (currentTaskId == null) {
            AlertUtil.showError(ErrorMessageConstant.NO_TASK_TO_EXPORT);
            return;
        }
        String taskId = currentTaskId;
        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
            @Override protected String call() { return reviewService.exportExcel(taskId); }
        };
        task.setOnSucceeded(e -> {
            String path = task.getValue();
            if (path != null) {
                AlertUtil.showInfo("Excel 导出成功", "文件路径: " + path);
            } else {
                AlertUtil.showError(ErrorMessageConstant.EXPORT_DATA_NOT_FOUND);
            }
        });
        task.setOnFailed(e -> AlertUtil.showError("导出失败: " + task.getException().getMessage()));
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML void handleExportWord() {
        if (currentTaskId == null) {
            AlertUtil.showError(ErrorMessageConstant.NO_TASK_TO_EXPORT);
            return;
        }
        String taskId = currentTaskId;
        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
            @Override protected String call() { return reviewService.exportWord(taskId); }
        };
        task.setOnSucceeded(e -> {
            String path = task.getValue();
            if (path != null) {
                AlertUtil.showInfo("Word 导出成功", "文件路径: " + path);
            } else {
                AlertUtil.showError(ErrorMessageConstant.EXPORT_DATA_NOT_FOUND);
            }
        });
        task.setOnFailed(e -> AlertUtil.showError("导出失败: " + task.getException().getMessage()));
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // 弹窗显示视图并刷新数据
    private <T> void showView(String viewPath, String title, Consumer<T> controllerConsumer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/javascene/gradingfx/" + viewPath));
            Parent root = loader.load();
            T controller = loader.getController();
            controllerConsumer.accept(controller);

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            refresh();
        } catch (IOException e) {
            log.error("加载视图失败: {}", e.getMessage(), e);
            AlertUtil.showError(ErrorMessageConstant.VIEW_LOAD_FAILED);
        }
    }

    private void refresh() {
        studentData.clear();
        loadData();
    }
}