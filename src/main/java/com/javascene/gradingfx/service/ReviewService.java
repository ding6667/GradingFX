package com.javascene.gradingfx.service;

import com.javascene.gradingfx.model.GradingTask;
import com.javascene.gradingfx.model.StudentResultProperty;

import java.util.List;
import java.util.Map;

public interface ReviewService {
    Map<String, List<String>> wordConvertToMd(List<String> fileUrls);
    Map<String, String> mdConvertToWord(String mdContent);
    List<GradingTask> loadAllTasks();
    List<StudentResultProperty> loadTask(String taskId);

    String runWorkflowWithCommonFiles(List<String> fileUrls, String rubric);
    String runWorkflowWithProjectZip(String zipFilePath, String rubric);
}
