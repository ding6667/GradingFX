package com.javascene.gradingfx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;

import java.io.File;

public class ExportController {

    @FXML private RadioButton excelRadio;
    @FXML private RadioButton wordRadio;
    @FXML private ToggleGroup formatGroup;
    @FXML private TextField pathField;
    @FXML private Button browseBtn;
    @FXML private TextField fileNameField;
    @FXML private CheckBox includeNotesCheck;
    @FXML private Button exportBtn;
    @FXML private Button cancelBtn;

    @FXML
    public void initialize() {
        excelRadio.setSelected(true);
        fileNameField.setPromptText("例如: 作业成绩_{date}_{class}");
    }

    @FXML void handleBrowse() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("选择导出目录");
        File dir = dirChooser.showDialog(pathField.getScene().getWindow());
        if (dir != null) {
            pathField.setText(dir.getAbsolutePath());
        }
    }

    @FXML void handleExport() {
        String format = excelRadio.isSelected() ? "Excel (.xlsx)" : "Word (.docx)";
        System.out.println("Exporting as " + format + " to: " + pathField.getText());
        System.out.println("File template: " + fileNameField.getText());
        System.out.println("Include notes: " + includeNotesCheck.isSelected());
    }

    @FXML void handleCancel() {
        System.out.println("Export cancelled");
    }
}