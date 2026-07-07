package com.javascene.gradingfx.service;

import com.javascene.gradingfx.model.HistoryTask;
import com.javascene.gradingfx.model.StudentResult;

import java.util.List;

public interface HistoryService {
    List<HistoryTask> loadAllTasks();
    List<StudentResult> loadStudentResults(String taskId);
}
