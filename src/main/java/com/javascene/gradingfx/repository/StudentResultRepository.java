package com.javascene.gradingfx.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.javascene.gradingfx.config.property.DataConfig;
import com.javascene.gradingfx.model.StudentResult;
import com.javascene.gradingfx.util.ConfigLoader;
import com.javascene.gradingfx.util.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生结果仓库 —— 负责按 taskId 读写 JSON 文件
 *
 * 双文件策略：
 *   主文件  {data.task}/task_{taskId}.json          — 轻量，不含 wordContent（供 Excel/UI 读取）
 *   内容文件 {data.task}/task_{taskId}_content.json — 仅 studentId → wordContent（供重试加载）
 */
@Slf4j
public class StudentResultRepository {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 主文件路径（轻量，不含 wordContent）
     */
    private String getTaskJsonPath(String taskId) {
        DataConfig data = ConfigLoader.getConfig().getData();
        return data.getTask() + File.separator + "task_" + taskId + ".json";
    }

    /**
     * 内容文件路径（仅 studentId → wordContent）
     */
    private String getTaskContentPath(String taskId) {
        DataConfig data = ConfigLoader.getConfig().getData();
        return data.getTask() + File.separator + "task_" + taskId + "_content.json";
    }

    /**
     * 按 taskId 加载全部学生结果（轻量，不含 wordContent）
     * 用于 Excel 导出、UI 显示等场景
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
     * 按 taskId 加载全部学生结果（含 wordContent，合并两个文件）
     * 用于重试场景——需要 wordContent 重新调用 Dify
     */
    public List<StudentResult> loadByTaskIdWithContent(String taskId) {
        List<StudentResult> students = loadByTaskId(taskId);
        if (students.isEmpty()) {
            return students;
        }

        String contentPath = getTaskContentPath(taskId);
        if (!FileUtil.exists(contentPath)) {
            log.warn("内容文件不存在: {}，wordContent 将为空", contentPath);
            return students;
        }

        try {
            // 读取 content 文件：Map<studentId, wordContent>
            Map<String, String> contentMap = mapper.readValue(new File(contentPath),
                    mapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, String.class));
            // 回填 wordContent
            for (StudentResult s : students) {
                s.setWordContent(contentMap.get(s.getStudentId()));
            }
        } catch (IOException e) {
            log.error("加载内容文件失败 [{}]: {}", contentPath, e.getMessage());
        }
        return students;
    }

    /**
     * 按 taskId 保存全部学生结果（双文件覆盖写入）
     * 主文件：不含 wordContent 的轻量列表
     * 内容文件：studentId → wordContent 映射
     */
    public void saveByTaskId(String taskId, List<StudentResult> students) {
        String mainPath = getTaskJsonPath(taskId);
        String contentPath = getTaskContentPath(taskId);

        try {
            File parent = new File(mainPath).getParentFile();
            if (parent != null) {
                FileUtil.ensureDirExists(parent.getAbsolutePath());
            }

            // 1. 主文件：序列化后移除 wordContent
            ArrayNode mainArray = mapper.createArrayNode();
            Map<String, String> contentMap = new LinkedHashMap<>();

            for (StudentResult s : students) {
                ObjectNode node = mapper.valueToTree(s);
                node.remove("wordContent");
                mainArray.add(node);

                if (s.getWordContent() != null) {
                    contentMap.put(s.getStudentId(), s.getWordContent());
                }
            }

            FileUtil.writeJson(mainPath, mainArray);
            FileUtil.writeJson(contentPath, contentMap);

            log.debug("任务 JSON 已保存: {} (共 {} 条), 内容文件: {}",
                    mainPath, students.size(), contentPath);
        } catch (IOException e) {
            log.error("保存任务 JSON 失败 [{}]: {}", mainPath, e.getMessage());
        }
    }

    /**
     * 判断任务 JSON 是否存在
     */
    public boolean exists(String taskId) {
        return FileUtil.exists(getTaskJsonPath(taskId));
    }
}
