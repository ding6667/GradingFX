package com.javascene.gradingfx.controller;

import com.javascene.gradingfx.constant.ErrorMessageConstant;
import com.javascene.gradingfx.model.StudentResult;
import com.javascene.gradingfx.model.StudentResultProperty;
import com.javascene.gradingfx.repository.StudentResultRepository;
import com.javascene.gradingfx.service.Impl.ReviewServiceImpl;
import com.javascene.gradingfx.service.ReviewService;
import com.javascene.gradingfx.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
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

    private StudentResultProperty currentResult;
    private String taskId;

    private final StudentResultRepository repository = new StudentResultRepository();
    private final ReviewService reviewService = new ReviewServiceImpl();

    @FXML
    public void initialize() {
    }

    public void setStudentResult(StudentResultProperty result, String taskId) {
        this.currentResult = result;
        this.taskId = taskId;
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
        if (currentResult == null || taskId == null) {
            return;
        }

        String teacherScore = teacherScoreField.getText().trim();
        String teacherComment = teacherCommentArea.getText().trim();
        String teacherNote = teacherNoteArea.getText().trim();

        // 校验分数格式（非空时必须是 0-100 的数字）
        if (!teacherScore.isEmpty()) {
            try {
                int score = Integer.parseInt(teacherScore);
                if (score < 0 || score > 100) {
                    AlertUtil.showWarning("分数范围错误", "教师得分应在 0-100 之间");
                    return;
                }
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("分数格式错误", "教师得分必须为数字");
                return;
            }
        }

        try {
            List<StudentResult> students = repository.loadByTaskId(taskId);
            if (students.isEmpty()) {
                AlertUtil.showError(ErrorMessageConstant.EXPORT_DATA_NOT_FOUND);
                return;
            }

            String studentId = currentResult.getId();
            boolean found = false;
            for (StudentResult s : students) {
                if (studentId.equals(s.getStudentId())) {
                    s.setTeacherScore(teacherScore);
                    s.setTeacherComment(teacherComment);
                    s.setTeacherNote(teacherNote);
                    found = true;
                    break;
                }
            }

            if (!found) {
                log.warn("未在 JSON 中找到学生: {}", studentId);
                AlertUtil.showError("保存失败", "未找到学生记录: " + studentId);
                return;
            }

            repository.saveByTaskId(taskId, students);

            // 标记分数已变更，下次导出 Excel 时重新生成
            reviewService.markScoreChanged(taskId);

            log.info("教师复核已保存: studentId={}, score={}", studentId, teacherScore);
            AlertUtil.showSuccess("保存成功", "教师复核信息已保存");

            ((Stage) saveBtn.getScene().getWindow()).close();
        } catch (Exception e) {
            log.error("保存教师复核失败: {}", e.getMessage(), e);
            AlertUtil.showError("保存失败", e.getMessage());
        }
    }

    @FXML void handleCancel() {
        ((Stage) cancelBtn.getScene().getWindow()).close();
    }
}
