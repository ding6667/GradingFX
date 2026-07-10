package com.javascene.gradingfx.service;

import com.javascene.gradingfx.model.HistoryTask;
import com.javascene.gradingfx.model.StudentResultProperty;

import java.util.List;

public interface HistoryService {
    List<HistoryTask> loadAllTasks();
    List<StudentResultProperty> loadStudentResults(String taskId);
    /** 删除指定任务（从 totalTask.json 移除并清理关联文件） */
    List<HistoryTask> deleteTask(String taskId);
}
