package com.javascene.gradingfx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;
import java.io.File;
import java.util.List;
import javafx.scene.control.ChoiceDialog;
import java.util.List;
import java.util.Optional;
import com.javascene.gradingfx.constant.ErrorMessageConstant;
import javafx.stage.Stage;
import com.javascene.gradingfx.util.FileUtil;
import com.javascene.gradingfx.model.StandardConfig;
import com.javascene.gradingfx.util.ConfigLoader;
import com.javascene.gradingfx.config.property.AppConfig;
import static com.javascene.gradingfx.util.FileUtil.writeJson;

import com.javascene.gradingfx.service.StandardService;
import com.javascene.gradingfx.service.Impl.StandardServiceImpl;
import com.javascene.gradingfx.constant.ErrorMessageConstant;
import com.javascene.gradingfx.util.AlertUtil;
public class StandardController {
    private final StandardService standardService = new StandardServiceImpl();
    @FXML private TextArea standardTextArea;
    @FXML private TextField templateNameField;
    @FXML private ComboBox<String> templateCombo;
    @FXML private Button loadDefaultBtn;
    @FXML private Button saveTemplateBtn;
    @FXML private Button loadTemplateBtn;
    @FXML private Button deleteTemplateBtn;
    @FXML private Button saveCloseBtn;
    @FXML private Button cancelBtn;

    @FXML
    public void initialize() {
        // 从service拿所有模板名称
        List<String> nameList = standardService.getAllTemplateNames();
        templateCombo.getItems().addAll(nameList);

        // 第四条需求：启动加载默认标准，不再写死字符串
        try {
            String defaultText = standardService.getCurrentStandard();
            standardTextArea.setText(defaultText);
        } catch (Exception e) {
            AlertUtil.showError(ErrorMessageConstant.LOAD_TEMPLATE_FAIL);
            e.printStackTrace();
        }
    }



    @FXML void handleLoadDefault() {
        try {
            String defaultText = standardService.getCurrentStandard();
            standardTextArea.setText(defaultText);
        } catch (Exception e) {
            AlertUtil.showError(ErrorMessageConstant.LOAD_TEMPLATE_FAIL);
        }
    }
    @FXML
    void loadOtherTemplate() {
        try {
            // 1.调用service获取全部自定义模板
            List<String> templateList = standardService.getAllSaveTemplateName();
            // 判断：没有任何模板
            if (templateList.isEmpty()) {
                AlertUtil.showInfo(ErrorMessageConstant.NO_SAVE_TEMPLATE);
                return;
            }

            // 2.弹出选择框，让用户选模板
            ChoiceDialog<String> dialog = new ChoiceDialog<>(templateList.get(0), templateList);
            dialog.setTitle("加载自定义模板");
            dialog.setHeaderText(ErrorMessageConstant.SELECT_TEMPLATE);
            dialog.setContentText("选择模板：");

            // 3.获取用户选中的模板名称
            Optional<String> selectName = dialog.showAndWait();
            if (selectName.isPresent()) {
                String templateText = standardService.loadOtherTemplateByName(selectName.get());
                // 4.回填到文本框
                standardTextArea.setText(templateText);
                AlertUtil.showInfo(ErrorMessageConstant.LOAD_OTHER_TEMPLATE_SUCCESS);
            }
        } catch (Exception e) {
            // 统一错误弹窗
            AlertUtil.showError(ErrorMessageConstant.LOAD_TEMPLATE_FAIL);
            e.printStackTrace();
        }
    }


    @FXML
    void saveCurrentStandard() {
        String inputText = standardTextArea.getText();
        try {
            standardService.saveCurrentStandard(inputText);
            AlertUtil.showInfo(ErrorMessageConstant.CURRENT_SAVE_SUCCESS);
        } catch (Exception e) {
            AlertUtil.showError(ErrorMessageConstant.CURRENT_SAVE_FAIL);
            e.printStackTrace();
        }
    }

    @FXML
    void handleDeleteTemplate() {
        String selected = templateCombo.getValue();
        if (selected == null || selected.isBlank()) {
            AlertUtil.showError(ErrorMessageConstant.DELETE_NO_SELECT);
            return;
        }
        boolean success = standardService.deleteTemplate(selected);
        if (success) {
            AlertUtil.showInfo(ErrorMessageConstant.DELETE_SUCCESS);
            templateCombo.getItems().remove(selected);
            templateCombo.setValue(null);
        } else {
            AlertUtil.showError(ErrorMessageConstant.DELETE_FAIL);
        }
    }
    @FXML void handleSaveTemplate() {
        String name = templateNameField.getText();
        // 判空，弹窗提示
        if (name == null || name.isBlank()) {
            AlertUtil.showError(ErrorMessageConstant.TEMPLATE_NAME_EMPTY);
            return;
        }
        String text = standardTextArea.getText();
        try {
            standardService.saveCustomTemplate(name, text);
            AlertUtil.showInfo(ErrorMessageConstant.SAVE_TEMPLATE_SUCCESS);
            // 下拉框新增名称
            templateCombo.getItems().add(name);
            templateCombo.setValue(name);
            templateNameField.clear();
        } catch (Exception e) {
            AlertUtil.showError(ErrorMessageConstant.SAVE_TEMPLATE_FAIL);
            e.printStackTrace();
        }
    }

    @FXML void handleLoadTemplate() {
        String selected = templateCombo.getValue();
        if (selected == null || selected.isBlank()) {
            AlertUtil.showError(ErrorMessageConstant.NO_SELECT_TEMPLATE);
            return;
        }
        try {
            String content = standardService.loadTemplate(selected);
            if (content == null) {
                AlertUtil.showError(ErrorMessageConstant.TEMPLATE_FILE_MISS);
                return;
            }
            standardTextArea.setText(content);
        } catch (Exception e) {
            AlertUtil.showError(ErrorMessageConstant.LOAD_TEMPLATE_FAIL);
            e.printStackTrace();
        }
    }



    @FXML void handleSaveAndClose() {
        saveCurrentStandard();
        // 获取当前弹窗并关闭
        Stage window = (Stage) standardTextArea.getScene().getWindow();
        window.close();
    }

    @FXML void handleCancel() {
        Stage window = (Stage) standardTextArea.getScene().getWindow();
        window.close();
    }
}

