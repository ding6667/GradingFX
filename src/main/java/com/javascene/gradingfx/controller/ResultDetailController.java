package com.javascene.gradingfx.controller;

import com.javascene.gradingfx.model.StudentResult;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ResultDetailController {

    @FXML private Label studentNameLabel;
    @FXML private Label studentIdLabel;
    @FXML private Label aiScoreLabel;
    @FXML private TextArea aiCommentArea;
    @FXML private TextField teacherScoreField;
    @FXML private TextArea teacherCommentArea;
    @FXML private TextArea teacherNoteArea;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;

    private StudentResult currentResult;

    @FXML
    public void initialize() {
    }

    public void setStudentResult(StudentResult result) {
        this.currentResult = result;
        if (result != null) {
            studentNameLabel.setText(result.getName());
            studentIdLabel.setText(result.getId());
            aiScoreLabel.setText(result.getRawScore());
            aiCommentArea.setText(result.getAiComment());
            teacherScoreField.setText(result.getTeacherScore());
            teacherCommentArea.setText(result.getTeacherComment());
            teacherNoteArea.setText(result.getTeacherNote());
        }
    }

    @FXML void handleSave() {
        if (currentResult != null) {
            currentResult.setTeacherScore(teacherScoreField.getText());
            currentResult.setTeacherComment(teacherCommentArea.getText());
            currentResult.setTeacherNote(teacherNoteArea.getText());
            System.out.println("Saved review for: " + currentResult.getName());
        }
    }

    @FXML void handleCancel() {
        System.out.println("Cancelled");
    }
}
