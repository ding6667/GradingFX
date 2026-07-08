package com.javascene.gradingfx.model;

import com.javascene.gradingfx.enmu.ReviewStatus;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.Getter;
import lombok.Setter;

public class StudentResultProperty {
    @Setter
    @Getter
    private String taskId; // 任务ID
    private SimpleStringProperty studentId; // 学生ID
    private SimpleStringProperty studentName; // 学生姓名
    private SimpleStringProperty rawScore; // AI原始分数
    private SimpleStringProperty aiComment; // AI批阅评论
    private SimpleStringProperty teacherScore; // 教师分数
    private SimpleStringProperty teacherComment; // 教师批阅评论
    private SimpleStringProperty teacherNote; // 教师备注
    private SimpleObjectProperty<ReviewStatus> status; // 状态
    private SimpleStringProperty errorMessage; // 错误信息

    public StudentResultProperty() {
        this.taskId = null;
        this.studentId = new SimpleStringProperty();
        this.studentName = new SimpleStringProperty();
        this.rawScore = new SimpleStringProperty();
        this.aiComment = new SimpleStringProperty();
        this.teacherScore = new SimpleStringProperty();
        this.teacherComment = new SimpleStringProperty();
        this.teacherNote = new SimpleStringProperty();
        this.status = new SimpleObjectProperty<>();
        this.errorMessage = new SimpleStringProperty();
    }

    public StudentResultProperty(String taskId, String id, String name, String rawScore, String aiComment, String teacherScore, String teacherComment, String teacherNote, String status, String errorMessage) {
        this.taskId = taskId;
        this.studentId = new SimpleStringProperty(id);
        this.studentName = new SimpleStringProperty(name);
        this.rawScore = new SimpleStringProperty(rawScore);
        this.aiComment = new SimpleStringProperty(aiComment);
        this.teacherScore = new SimpleStringProperty(teacherScore);
        this.teacherComment = new SimpleStringProperty(teacherComment);
        this.teacherNote = new SimpleStringProperty(teacherNote);
        ReviewStatus rs;
        try {
            rs = (status != null) ? ReviewStatus.valueOf(status) : ReviewStatus.PENDING;
        } catch (IllegalArgumentException e) {
            rs = ReviewStatus.PENDING;
        }
        this.status = new SimpleObjectProperty<>(rs);
        this.errorMessage = new SimpleStringProperty(errorMessage);
    }

    public StudentResultProperty(String taskId, String id, String name, String rawScore, String aiComment, String teacherScore, String teacherComment, String teacherNote, ReviewStatus status, String errorMessage) {
        this.taskId = taskId;
        this.studentId = new SimpleStringProperty(id);
        this.studentName = new SimpleStringProperty(name);
        this.rawScore = new SimpleStringProperty(rawScore);
        this.aiComment = new SimpleStringProperty(aiComment);
        this.teacherScore = new SimpleStringProperty(teacherScore);
        this.teacherComment = new SimpleStringProperty(teacherComment);
        this.teacherNote = new SimpleStringProperty(teacherNote);
        this.status = new SimpleObjectProperty<>(status != null ? status : ReviewStatus.PENDING);
        this.errorMessage = new SimpleStringProperty(errorMessage);
    }

    public String getId() { return studentId.get(); }
    public void setId(String id) { this.studentId.set(id); }
    public SimpleStringProperty idProperty() { return studentId; }

    public String getName() { return studentName.get(); }
    public void setName(String name) { this.studentName.set(name); }
    public SimpleStringProperty nameProperty() { return studentName; }

    public String getRawScore() { return rawScore.get(); }
    public void setRawScore(String rawScore) { this.rawScore.set(rawScore); }
    public SimpleStringProperty rawScoreProperty() { return rawScore; }

    public String getAiComment() { return aiComment.get(); }
    public void setAiComment(String aiComment) { this.aiComment.set(aiComment); }
    public SimpleStringProperty aiCommentProperty() { return aiComment; }

    public ReviewStatus getStatus() { return status.getValue(); }
    public void setStatus(ReviewStatus status) { this.status.set(status); }
    public SimpleObjectProperty<ReviewStatus> statusProperty() { return status; }

    public String getTeacherScore() { return teacherScore.get(); }
    public void setTeacherScore(String teacherScore) { this.teacherScore.set(teacherScore); }
    public SimpleStringProperty teacherScoreProperty() { return teacherScore; }

    public String getTeacherComment() { return teacherComment.get(); }
    public void setTeacherComment(String teacherComment) { this.teacherComment.set(teacherComment); }
    public SimpleStringProperty teacherCommentProperty() { return teacherComment; }

    public String getTeacherNote() { return teacherNote.get(); }
    public void setTeacherNote(String teacherNote) { this.teacherNote.set(teacherNote); }
    public SimpleStringProperty teacherNoteProperty() { return teacherNote; }

    public String getErrorMessage() { return errorMessage.get(); }
    public void setErrorMessage(String errorMessage) { this.errorMessage.set(errorMessage); }
    public SimpleStringProperty errorMessageProperty() { return errorMessage; }


}
