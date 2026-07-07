package com.javascene.gradingfx.controller;

import com.javascene.gradingfx.model.StudentResult;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class MainController {

    @FXML private StackPane uploadArea;
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

    @FXML private TableView<StudentResult> resultTable;
    @FXML private TableColumn<StudentResult, String> idColumn;
    @FXML private TableColumn<StudentResult, String> nameColumn;
    @FXML private TableColumn<StudentResult, String> scoreColumn;
    @FXML private TableColumn<StudentResult, String> commentColumn;
    @FXML private TableColumn<StudentResult, String> statusColumn;

    private final ObservableList<StudentResult> studentData = FXCollections.observableArrayList();



    @FXML
    public void initialize() {
        // Setup table columns
        idColumn.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());


        // Load mock data
        loadMockData();
    }

    private void loadMockData() {
        studentData.addAll(List.of(
        ));

        resultTable.setItems(studentData);

        // Update stats
        totalLabel.setText(String.valueOf(studentData.size()));

        // Update progress


        // Setup tree
        setupStudentTree();
    }

    private void setupStudentTree() {
        TreeItem<String> root = new TreeItem<>("学生列表");
        root.setExpanded(true);

        TreeItem<String> pending = new TreeItem<>("⏳ 待批阅 (1)");
        pending.getChildren().add(new TreeItem<>("📄 周九 - 2021007"));

        TreeItem<String> completed = new TreeItem<>("✅ 已完成 (5)");
        completed.getChildren().add(new TreeItem<>("✅ 张三 - 2021001"));
        completed.getChildren().add(new TreeItem<>("✅ 李四 - 2021002"));
        completed.getChildren().add(new TreeItem<>("✅ 王五 - 2021003"));
        completed.getChildren().add(new TreeItem<>("✅ 钱七 - 2021005"));
        completed.getChildren().add(new TreeItem<>("✅ 孙八 - 2021006"));

        TreeItem<String> failed = new TreeItem<>("❌ 失败 (2)");
        failed.getChildren().add(new TreeItem<>("❌ 赵六 - 2021004"));
        failed.getChildren().add(new TreeItem<>("❌ 吴十 - 2021008"));

        root.getChildren().addAll(pending, completed, failed);
        studentTree.setRoot(root);
        studentTree.setShowRoot(false);
    }

    @FXML void handleOpenStandard() {
        // Open standard view
    }

    @FXML void handleOpenHistory() {
        // Open history view
    }

    @FXML void handleUploadClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP Files", "*.zip"));
        File file = fileChooser.showOpenDialog(uploadArea.getScene().getWindow());
        if (file != null) {
            System.out.println("Selected: " + file.getName());
        }
    }

    @FXML void handleDragOver(javafx.scene.input.DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
    }

    @FXML void handleDrop(javafx.scene.input.DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            List<File> files = event.getDragboard().getFiles();
            files.forEach(f -> System.out.println("Dropped: " + f.getName()));
            event.setDropCompleted(true);
            event.consume();
        }
    }

    @FXML void handleStart() {
        System.out.println("开始批阅");
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
        StudentResult selected = resultTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            System.out.println("View detail for: " + selected.getName());
        }
    }

    @FXML void handleExportExcel() {
        System.out.println("Export to Excel");
    }

    @FXML void handleExportWord() {
        System.out.println("Export to Word");
    }

    // 弹窗显示视图并刷新数据
    private <T> void showView(String viewPath, String title, Consumer<T> controllerConsumer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(viewPath));
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

    }
}
