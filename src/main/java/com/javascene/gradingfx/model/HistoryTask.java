package com.javascene.gradingfx.model;

import com.javascene.gradingfx.enmu.HistoryStatus;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

public class HistoryTask {
    private SimpleStringProperty taskId;// 任务ID
    private SimpleStringProperty taskName;// 任务名称
    private SimpleStringProperty totalStudents;// 总学生数
    private SimpleStringProperty gradedStudents;// 已评分学生数
    private SimpleObjectProperty<HistoryStatus> status;// 任务状态
    private SimpleStringProperty createTime;// 创建时间

    public HistoryTask() {

    }
    public HistoryTask(String taskId, String taskName, String totalStudents, String gradedStudents, HistoryStatus status, String createTime) {
        this.taskId = new SimpleStringProperty(taskId);
        this.taskName = new SimpleStringProperty(taskName);
        this.totalStudents = new SimpleStringProperty(totalStudents);
        this.gradedStudents = new SimpleStringProperty(gradedStudents);
        this.status = new SimpleObjectProperty<>(status);
        this.createTime = new SimpleStringProperty(createTime);
    }

    public String getTaskId() {
        return taskId.get();
    }

    public SimpleStringProperty taskIdProperty() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId.set(taskId);
    }

    public String getTaskName() {
        return taskName.get();
    }

    public SimpleStringProperty taskNameProperty() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName.set(taskName);
    }

    public String getTotalStudents() {
        return totalStudents.get();
    }

    public SimpleStringProperty totalStudentsProperty() {
        return totalStudents;
    }

    public void setTotalStudents(String totalStudents) {
        this.totalStudents.set(totalStudents);
    }

    public String getGradedStudents() {
        return gradedStudents.get();
    }

    public SimpleStringProperty gradedStudentsProperty() {
        return gradedStudents;
    }

    public void setGradedStudents(String gradedStudents) {
        this.gradedStudents.set(gradedStudents);
    }

    public HistoryStatus getStatus() {
        return status.get();
    }

    public SimpleObjectProperty<HistoryStatus> statusProperty() {
        return status;
    }

    public void setStatus(HistoryStatus status) {
        this.status.set(status);
    }

    public String getCreateTime() {
        return createTime.get();
    }

    public SimpleStringProperty createTimeProperty() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime.set(createTime);
    }
}