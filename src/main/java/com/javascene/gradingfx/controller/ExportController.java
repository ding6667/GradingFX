package com.javascene.gradingfx.controller;

import com.javascene.gradingfx.model.ExportConfig;
import com.javascene.gradingfx.util.ConfigLoader;
import com.javascene.gradingfx.util.FileUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

@Slf4j
public class ExportController {

    @FXML private TextField pathField;
    @FXML private Button browseBtn;
    @FXML private TextField fileNameField;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;

    @FXML
    public void initialize() {
        // 加载已有配置
        String configPath = ConfigLoader.getConfig().getData().getExportConfig();
        if (configPath != null && FileUtil.exists(configPath)) {
            try {
                ExportConfig config = FileUtil.readJson(configPath, ExportConfig.class);
                if (config != null) {
                    pathField.setText(config.getOutputPath() != null ? config.getOutputPath() : "");
                    fileNameField.setText(config.getFileNameTemplate() != null ? config.getFileNameTemplate() : "");
                }
            } catch (IOException e) {
                log.error("加载导出配置失败: {}", e.getMessage());
            }
        }
        fileNameField.setPromptText("例如: 作业成绩_{date}");
    }

    @FXML
    void handleBrowse() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("选择导出目录");
        Stage stage = (Stage) browseBtn.getScene().getWindow();
        File dir = dirChooser.showDialog(stage);
        if (dir != null) {
            pathField.setText(dir.getAbsolutePath());
        }
    }

    @FXML
    void handleSave() {
        ExportConfig config = new ExportConfig();
        config.setOutputPath(pathField.getText().trim());
        config.setFileNameTemplate(fileNameField.getText().trim());

        String configPath = ConfigLoader.getConfig().getData().getExportConfig();
        if (configPath == null || configPath.isEmpty()) {
            log.error("未配置 exportConfig 路径");
            return;
        }

        try {
            File parentDir = new File(configPath).getParentFile();
            if (parentDir != null) {
                FileUtil.ensureDirExists(parentDir.getAbsolutePath());
            }
            FileUtil.writeJson(configPath, config);
            log.info("导出配置已保存: path={}, template={}", config.getOutputPath(), config.getFileNameTemplate());
        } catch (IOException e) {
            log.error("保存导出配置失败: {}", e.getMessage());
        }

        // 关闭窗口
        Stage stage = (Stage) saveBtn.getScene().getWindow();
        stage.close();
    }

    @FXML
    void handleCancel() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
}
