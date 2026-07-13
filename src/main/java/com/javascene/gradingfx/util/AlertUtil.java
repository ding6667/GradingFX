package com.javascene.gradingfx.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;


public class AlertUtil {

    private static final String STYLE_SHEET = AlertUtil.class.getResource("alert-styles.css") != null
            ? AlertUtil.class.getResource("alert-styles.css").toExternalForm()
            : null;

    // ==================== 信息弹窗 ====================


    public static void showInfo(String message) {
        showAlert(Alert.AlertType.INFORMATION, "信息", message);
    }

    public static void showInfo(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message);
    }

    /**
     * 显示成功提示弹窗
     */
    public static void showSuccess(String message) {
        showAlert(Alert.AlertType.INFORMATION, "成功", message);
    }

    /**
     * 显示成功提示弹窗（自定义标题）
     */
    public static void showSuccess(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message);
    }


    /**
     * 显示警告弹窗
     */
    public static void showWarning(String message) {
        showAlert(Alert.AlertType.WARNING, "警告", message);
    }

    /**
     * 显示警告弹窗（自定义标题）
     */
    public static void showWarning(String title, String message) {
        showAlert(Alert.AlertType.WARNING, "⚠️ " + title, message);
    }


    /**
     * 显示错误弹窗
     */
    public static void showError(String message) {
        showAlert(Alert.AlertType.ERROR, "错误", message);
    }

    /**
     * 显示错误弹窗（自定义标题）
     */
    public static void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }

    /**
     * 显示错误弹窗（包含异常信息）
     */
    public static void showError(String message, Throwable exception) {
        showAlert(Alert.AlertType.ERROR, "错误", message, exception);
    }

    /**
     * 显示确认弹窗，返回用户选择
     */
    public static boolean showConfirm(String message) {
        return showConfirm("确认", message);
    }

    /**
     * 显示确认弹窗（自定义标题），返回用户选择
     */
    public static boolean showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleDialog(alert);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * 显示自定义按钮的确认弹窗
     */
    public static Optional<ButtonType> showConfirmWithButtons(String title, String message, ButtonType... buttonTypes) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getButtonTypes().setAll(buttonTypes);
        styleDialog(alert);

        return alert.showAndWait();
    }

    /**
     * 显示输入弹窗
     */
    public static Optional<String> showInput(String title, String message, String defaultValue) {
        TextInputDialog textInputDialog = new TextInputDialog(defaultValue);
        textInputDialog.setTitle(title);
        textInputDialog.setHeaderText(message);
        textInputDialog.setContentText("");
        styleDialog(textInputDialog);

        return textInputDialog.showAndWait();
    }


    private static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleDialog(alert);
        alert.showAndWait();
    }

    private static void showAlert(Alert.AlertType type, String title, String message, Throwable exception) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(message);

        StringBuilder detailText = new StringBuilder();
        if (exception != null) {
            detailText.append(exception.getClass().getSimpleName());
            detailText.append(": ").append(exception.getMessage());
        }

        alert.setContentText(detailText.toString());
        styleDialog(alert);
        alert.showAndWait();
    }

    /**
     * 统一弹窗样式
     */
    private static void styleDialog(Dialog<?> dialog) {
        DialogPane dialogPane = dialog.getDialogPane();

        // 应用自定义样式表
        if (STYLE_SHEET != null) {
            dialogPane.getStylesheets().add(STYLE_SHEET);
        }
    }

    /**
     * 创建自定义 Alert 实例（用于更复杂的场景）
     */
    public static Alert createAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        styleDialog(alert);
        return alert;
    }

    /**
     * 显示带超链接的确认弹窗
     */
    public static boolean showConfirmWithLink(String title, String message, String linkText, String url) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(message);

        javafx.scene.control.Label linkLabel = new javafx.scene.control.Label(linkText);
        linkLabel.setStyle("-fx-text-fill: #1890FF; -fx-underline: true; -fx-cursor: hand;");
        linkLabel.setOnMouseClicked(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        alert.getDialogPane().setContent(linkLabel);
        styleDialog(alert);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    public static void info(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    // 错误提示弹窗
    public static void error(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setContentText(msg);
        alert.showAndWait();
    }

}

