package com.javascene.gradingfx.service;

import com.javascene.gradingfx.model.HistoryTask;
import com.javascene.gradingfx.model.StudentResultProperty;

import java.util.List;

public interface HistoryService {
    List<HistoryTask> loadAllTasks();
    List<StudentResultProperty> loadStudentResults(String taskId);
    List<HistoryTask> deleteTask(String taskId);
}
