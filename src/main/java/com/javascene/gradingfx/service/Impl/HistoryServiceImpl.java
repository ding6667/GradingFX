package com.javascene.gradingfx.service.Impl;

import com.javascene.gradingfx.config.property.DataConfig;
import com.javascene.gradingfx.enmu.HistoryStatus;
import com.javascene.gradingfx.enmu.ReviewStatus;
import com.javascene.gradingfx.model.*;
import com.javascene.gradingfx.service.HistoryService;
import com.javascene.gradingfx.util.ConfigLoader;
import com.javascene.gradingfx.util.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class HistoryServiceImpl implements HistoryService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private String getTaskFilePath() {
        DataConfig data = ConfigLoader.getConfig().getData();
        return data.getTask() + File.separator + "tasks.json";
    }

    private String getScoreFilePath() {
        DataConfig data = ConfigLoader.getConfig().getData();
        return data.getResult() + File.separator + "scores.json";
    }

    @Override
    public List<HistoryTask> loadAllTasks() {
        String path = getTaskFilePath();
        if (!FileUtil.exists(path)) {
            return Collections.emptyList();
        }
        try {
            List<GradingTask> tasks = FileUtil.readJsonList(path, GradingTask.class);
            List<HistoryTask> result = new ArrayList<>();
            for (GradingTask t : tasks) {
                result.add(toHistoryTask(t));
            }
            return result;
        } catch (Exception e) {
            log.error("读取任务列表失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<StudentResultProperty> loadStudentResults(String taskId) {
        String path = getScoreFilePath();
        if (!FileUtil.exists(path)) {
            return Collections.emptyList();
        }
        try {
            List<StudentResult> scores = FileUtil.readJsonList(path, StudentResult.class);
            return scores.stream()
                    .filter(s -> taskId.equals(s.getTaskId()))
                    .map(this::toStudentResultProperty)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("读取学生成绩失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private HistoryTask toHistoryTask(GradingTask t) {
        HistoryStatus hs;
        switch (t.getStatus()) {
            case 0 -> hs = HistoryStatus.GRADING;
            case 2 -> hs = HistoryStatus.COMPLETED;
            case 4 -> hs = HistoryStatus.FAILED;
            default -> {
                log.warn("未知任务状态码: {} (task: {})", t.getStatus(), t.getId());
                hs = HistoryStatus.PENDING;
            }
        }
        String time = t.getCreateTime() != null ? t.getCreateTime().format(FMT) : "";
        String name = t.getTaskName() != null ? t.getTaskName() : t.getId();
        return new HistoryTask(
                t.getId(),
                name,
                String.valueOf(t.getTotalStudents()),
                String.valueOf(t.getGradedStudents()),
                hs,
                time
        );
    }

    private StudentResultProperty toStudentResultProperty(StudentResult s) {
        String statusStr = s.getStatus() != null ? s.getStatus() : "PENDING";
        ReviewStatus rs;
        try {
            rs = ReviewStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            rs = ReviewStatus.PENDING;
        }
        return new StudentResultProperty(
                s.getTaskId(),
                s.getStudentId(),
                s.getStudentName(),
                s.getRawScore(),
                s.getAiComment(),
                s.getTeacherScore(),
                s.getTeacherComment(),
                s.getTeacherNote(),
                rs,
                s.getErrorMessage()
        );
    }
}
