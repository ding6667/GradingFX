package com.javascene.gradingfx.controller;

import com.javascene.gradingfx.config.property.AppConfig;
import com.javascene.gradingfx.constant.ErrorMessageConstant;
import com.javascene.gradingfx.enmu.ReviewStatus;
import com.javascene.gradingfx.model.GradingTask;
import com.javascene.gradingfx.model.HistoryTask;
import com.javascene.gradingfx.model.StudentResultProperty;
import com.javascene.gradingfx.service.HistoryService;
import com.javascene.gradingfx.service.Impl.HistoryServiceImpl;
import com.javascene.gradingfx.service.Impl.ReviewServiceImpl;
import com.javascene.gradingfx.service.Impl.StandardServiceImpl;
import com.javascene.gradingfx.service.ReviewService;
import com.javascene.gradingfx.service.StandardService;
import com.javascene.gradingfx.util.AlertUtil;
import com.javascene.gradingfx.util.ConfigLoader;
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

    @FXML void handleOpenStandard() {
        showView("standard-view.fxml", "设置评分标准",o -> {});
    }

    @FXML void handleOpenHistory() {
        showView("history-view.fxml", "查看历史记录",o -> {});
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
    }

    private void handleSelectedFile(File file) {
        if (!selectedFileObjects.contains(file)) {
            selectedFileObjects.add(file);
            updateSelectedFilesArea();
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
        // 默认批阅第一个
        reviewService.runWorkflowWithProjectZip(selectedFileObjects.get(0).getAbsolutePath(), standardService.getCurrentStandard());
        refresh();
    }

    @FXML void handlePause() {
        System.out.println("暂停批阅");
    }

    @FXML void handleResume() {
        System.out.println("继续批阅");
    }

    @FXML void handleRetry() {
        System.out.println("重试失败项");
    }

    @FXML void handleViewDetail() {
        StudentResultProperty selected = resultTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            System.out.println("View detail for: " + selected.getName());
        }
    }

    @FXML void handleExportExcel() {

    }

    @FXML void handleExportWord() {

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
            e.printStackTrace();
        }
    }

    private void refresh() {
        studentData.clear();
        loadData();
    }
}