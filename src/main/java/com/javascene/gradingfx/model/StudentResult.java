package com.javascene.gradingfx.model;

import com.javascene.gradingfx.enmu.ReviewStatus;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

public class StudentResult {
    private SimpleStringProperty id; // 学生ID
    private SimpleStringProperty name; // 学生姓名
    private SimpleStringProperty rawScore; // AI原始分数
    private SimpleStringProperty aiComment; // AI批阅评论
    private SimpleStringProperty teacherScore; // 教师分数
    private SimpleStringProperty teacherComment; // 教师批阅评论
    private SimpleStringProperty teacherNote; // 教师备注
    private SimpleObjectProperty<ReviewStatus> status; // 状态
    private SimpleStringProperty errorMessage; // 错误信息

    public StudentResult() {

    }

    public StudentResult(String id, String name, String rawScore, String aiComment, String teacherScore, String teacherComment, String teacherNote, String status, String errorMessage) {
        this.id = new SimpleStringProperty(id);
        this.name = new SimpleStringProperty(name);
        this.rawScore = new SimpleStringProperty(rawScore);
        this.aiComment = new SimpleStringProperty(aiComment);
        this.teacherScore = new SimpleStringProperty(teacherScore);
        this.teacherComment = new SimpleStringProperty(teacherComment);
        this.teacherNote = new SimpleStringProperty(teacherNote);
        this.status = new SimpleObjectProperty<>(ReviewStatus.valueOf(status));
        this.errorMessage = new SimpleStringProperty(errorMessage);

    }

    public String getId() { return id.get(); }
    public void setId(String id) { this.id.set(id); }
    public SimpleStringProperty idProperty() { return id; }

    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }
    public SimpleStringProperty nameProperty() { return name; }

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