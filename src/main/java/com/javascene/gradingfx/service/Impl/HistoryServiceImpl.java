package com.javascene.gradingfx.service.Impl;

import com.javascene.gradingfx.config.property.DataConfig;
import com.javascene.gradingfx.enmu.HistoryStatus;
import com.javascene.gradingfx.enmu.ReviewStatus;
import com.javascene.gradingfx.model.*;
import com.javascene.gradingfx.repository.StudentResultRepository;
import com.javascene.gradingfx.service.HistoryService;
import com.javascene.gradingfx.util.ConfigLoader;
import com.javascene.gradingfx.util.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class HistoryServiceImpl implements HistoryService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final StudentResultRepository studentRepository = new StudentResultRepository();

    private String getTotalTaskFilePath() {
        DataConfig data = ConfigLoader.getConfig().getData();
        return data.getTotalTask();
    }

    @Override
    public List<HistoryTask> loadAllTasks() {
        String path = getTotalTaskFilePath();
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
        try {
            List<StudentResult> results = studentRepository.loadByTaskId(taskId);
            List<StudentResultProperty> props = new ArrayList<>();
            for (StudentResult s : results) {
                props.add(toStudentResultProperty(s));
            }
            return props;
        } catch (Exception e) {
            log.error("读取学生成绩失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<HistoryTask> deleteTask(String taskId) {
        String path = getTotalTaskFilePath();
        if (!FileUtil.exists(path)) {
            return Collections.emptyList();
        }
        try {
            List<GradingTask> tasks = FileUtil.readJsonList(path, GradingTask.class);
            tasks.removeIf(t -> taskId.equals(t.getId()));
            FileUtil.writeJsonList(path, tasks);

            // 清理关联的任务结果文件
            DataConfig data = ConfigLoader.getConfig().getData();
            String taskFile = data.getTask() + File.separator + "task_" + taskId + ".json";
            String contentFile = data.getTask() + File.separator + "task_" + taskId + "_content.json";
            try {
                if (FileUtil.exists(taskFile)) FileUtil.delete(taskFile);
                if (FileUtil.exists(contentFile)) FileUtil.delete(contentFile);
            } catch (Exception e) {
                log.warn("清理任务关联文件失败: {}", e.getMessage());
            }

            List<HistoryTask> result = new ArrayList<>();
            for (GradingTask t : tasks) {
                result.add(toHistoryTask(t));
            }
            return result;
        } catch (Exception e) {
            log.error("删除任务失败: {}", e.getMessage());
            return loadAllTasks();
        }
    }

    private HistoryTask toHistoryTask(GradingTask t) {
        HistoryStatus hs;
        switch (t.getStatus()) {
            case 0 -> hs = HistoryStatus.GRADING;
            case 2 -> hs = HistoryStatus.COMPLETED;
            case 3 -> hs = HistoryStatus.PARTIAL_FAILURE;
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
