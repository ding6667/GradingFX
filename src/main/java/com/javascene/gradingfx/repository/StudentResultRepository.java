package com.javascene.gradingfx.repository;

import com.javascene.gradingfx.config.property.DataConfig;
import com.javascene.gradingfx.model.StudentResult;
import com.javascene.gradingfx.util.ConfigLoader;
import com.javascene.gradingfx.util.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 学生结果仓库 —— 负责按 taskId 读写 JSON 文件
 * 文件路径：{data.task}/task_{taskId}.json
 */
@Slf4j
public class StudentResultRepository {

    /**
     * 获取任务 JSON 文件路径
     */
    private String getTaskJsonPath(String taskId) {
        DataConfig data = ConfigLoader.getConfig().getData();
        return data.getTask() + File.separator + "task_" + taskId + ".json";
    }

    /**
     * 按 taskId 加载全部学生结果
     */
    public List<StudentResult> loadByTaskId(String taskId) {
        String path = getTaskJsonPath(taskId);
        if (!FileUtil.exists(path)) {
            log.warn("任务 JSON 文件不存在: {}", path);
            return new ArrayList<>();
        }
        try {
            return FileUtil.readJsonList(path, StudentResult.class);
        } catch (IOException e) {
            log.error("加载任务 JSON 失败 [{}]: {}", path, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 按 taskId 保存全部学生结果（覆盖写入）
     */
    public void saveByTaskId(String taskId, List<StudentResult> students) {
        String path = getTaskJsonPath(taskId);
        try {
            File parent = new File(path).getParentFile();
            if (parent != null) {
                FileUtil.ensureDirExists(parent.getAbsolutePath());
            }
            FileUtil.writeJsonList(path, students);
            log.debug("任务 JSON 已保存: {} (共 {} 条)", path, students.size());
        } catch (IOException e) {
            log.error("保存任务 JSON 失败 [{}]: {}", path, e.getMessage());
        }
    }

    /**
     * 判断任务 JSON 是否存在
     */
    public boolean exists(String taskId) {
        return FileUtil.exists(getTaskJsonPath(taskId));
    }
}
