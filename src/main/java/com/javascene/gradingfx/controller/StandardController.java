package com.javascene.gradingfx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class StandardController {

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
        // Load mock templates
        templateCombo.getItems().addAll(
            "默认评分模板",
            "代码规范评分模板",
            "算法效率评分模板",
            "项目文档评分模板"
        );
        templateCombo.setValue("默认评分模板");
    }

    @FXML void handleLoadDefault() {
        standardTextArea.setText(
            "[Java作业评分标准]\n\n" +
            "一、代码规范（30分）\n" +
            "1. 命名规范（10分）：变量、方法、类名符合Java命名规范\n" +
            "2. 代码格式（10分）：缩进统一，空行合理，括号配对\n" +
            "3. 注释完整（10分）：关键逻辑有注释说明\n\n" +
            "二、功能实现（40分）\n" +
            "1. 基础功能（20分）：核心功能完整实现\n" +
            "2. 扩展功能（10分）：额外功能加分\n" +
            "3. 边界处理（10分）：异常情况处理得当\n\n" +
            "三、算法效率（20分）\n" +
            "1. 时间复杂度（10分）：算法效率合理\n" +
            "2. 空间复杂度（10分）：内存使用优化\n\n" +
            "四、测试与调试（10分）\n" +
            "1. 测试用例（5分）：测试覆盖全面\n" +
            "2. 可运行性（5分）：程序可正常编译运行"
        );
    }

    @FXML void handleSaveTemplate() {
        String name = templateNameField.getText();
        if (name != null && !name.isBlank()) {
            templateCombo.getItems().add(name);
            templateCombo.setValue(name);
            templateNameField.clear();
        }
    }

    @FXML void handleLoadTemplate() {
        String selected = templateCombo.getValue();
        if (selected != null) {
            System.out.println("Loading template: " + selected);
        }
    }

    @FXML void handleDeleteTemplate() {
        String selected = templateCombo.getValue();
        if (selected != null) {
            templateCombo.getItems().remove(selected);
        }
    }

    @FXML void handleSaveAndClose() {
        System.out.println("Saving standard and closing...");
    }

    @FXML void handleCancel() {
        System.out.println("Cancelled");
    }
}