package com.javascene.gradingfx.service;

import com.javascene.gradingfx.model.GradingResult;
import com.javascene.gradingfx.model.GradingTask;
import com.javascene.gradingfx.model.StudentResult;
import com.javascene.gradingfx.util.FileUtil;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 存储逻辑与数据完整性测试
 * 覆盖：FileUtil LocalDateTime 序列化、appendToJsonList 追加模式、scores 批量写入
 */
class StorageLogicTest {

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("grading_test_");
    }

    @AfterEach
    void tearDown() throws IOException {
        // 递归删除临时目录
        try (var walk = Files.walk(tempDir)) {
            walk.sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    // ========== appendToJsonList 辅助方法（与 ReviewServiceImpl 中一致） ==========

    private <T> void appendToJsonList(String filePath, T element, Class<T> clazz) throws IOException {
        List<T> list;
        if (FileUtil.exists(filePath)) {
            list = FileUtil.readJsonList(filePath, clazz);
        } else {
            list = new ArrayList<>();
        }
        list.add(element);
        FileUtil.writeJsonList(filePath, list);
    }

    // ========== 1. GradingTask LocalDateTime 序列化 ==========

    @Test
    @DisplayName("GradingTask 含 LocalDateTime 字段的 JSON 序列化与反序列化")
    void testGradingTaskLocalDateTimeSerialization() throws IOException {
        GradingTask task = new GradingTask();
        task.setId("task-001");
        task.setFileId("/path/to/file.zip");
        task.setTaskName("Java作业_2024秋_计科1班");
        task.setTotalStudents(45);
        task.setGradedStudents(43);
        task.setStatus(2);
        task.setCreateTime(LocalDateTime.of(2026, 7, 7, 14, 30, 0));
        task.setFinishTime(LocalDateTime.of(2026, 7, 7, 14, 35, 12));
        task.setErrorMessage(null);
        task.setRetryCount(0);

        String path = tempDir.resolve("task_dt.json").toString();
        FileUtil.writeJson(path, task);

        GradingTask loaded = FileUtil.readJson(path, GradingTask.class);
        assertEquals("task-001", loaded.getId());
        assertEquals("Java作业_2024秋_计科1班", loaded.getTaskName());
        assertEquals(45, loaded.getTotalStudents());
        assertEquals(43, loaded.getGradedStudents());
        assertEquals(2, loaded.getStatus());
        assertEquals(LocalDateTime.of(2026, 7, 7, 14, 30, 0), loaded.getCreateTime());
        assertEquals(LocalDateTime.of(2026, 7, 7, 14, 35, 12), loaded.getFinishTime());
    }

    // ========== 2. appendToJsonList 追加不覆盖 ==========

    @Test
    @DisplayName("tasks.json 多次追加不覆盖已有数据")
    void testAppendTaskNotOverwrite() throws IOException {
        String path = tempDir.resolve("tasks.json").toString();

        GradingTask t1 = new GradingTask();
        t1.setId("task-001");
        t1.setTaskName("第一次批阅");
        t1.setStatus(2);
        t1.setCreateTime(LocalDateTime.now());
        appendToJsonList(path, t1, GradingTask.class);

        GradingTask t2 = new GradingTask();
        t2.setId("task-002");
        t2.setTaskName("第二次批阅");
        t2.setStatus(4);
        t2.setCreateTime(LocalDateTime.now());
        appendToJsonList(path, t2, GradingTask.class);

        GradingTask t3 = new GradingTask();
        t3.setId("task-003");
        t3.setTaskName("第三次批阅");
        t3.setStatus(2);
        t3.setCreateTime(LocalDateTime.now());
        appendToJsonList(path, t3, GradingTask.class);

        List<GradingTask> tasks = FileUtil.readJsonList(path, GradingTask.class);
        assertEquals(3, tasks.size(), "应该有3条任务记录");
        assertEquals("task-001", tasks.get(0).getId());
        assertEquals("task-002", tasks.get(1).getId());
        assertEquals("task-003", tasks.get(2).getId());
        assertEquals("第一次批阅", tasks.get(0).getTaskName());
        assertEquals("第三次批阅", tasks.get(2).getTaskName());
    }

    // ========== 3. GradingResult 序列化 ==========

    @Test
    @DisplayName("results.json 追加 GradingResult 含路径和过期时间")
    void testAppendGradingResult() throws IOException {
        String path = tempDir.resolve("results.json").toString();

        GradingResult r1 = new GradingResult();
        r1.setTaskId("task-001");
        r1.setStatus(0);
        r1.setCreateTime(LocalDateTime.of(2026, 7, 7, 14, 35, 0));
        r1.setWordPath("/output/review.docx");
        r1.setExcelPath("/output/scores.xlsx");
        r1.setExpireTime(LocalDateTime.of(2026, 7, 14, 14, 35, 0));
        appendToJsonList(path, r1, GradingResult.class);

        GradingResult r2 = new GradingResult();
        r2.setTaskId("task-002");
        r2.setStatus(0);
        r2.setCreateTime(LocalDateTime.of(2026, 7, 7, 15, 0, 0));
        r2.setWordPath("/output/review2.docx");
        r2.setExcelPath(null); // excelPath 可以为 null
        r2.setExpireTime(LocalDateTime.of(2026, 7, 14, 15, 0, 0));
        appendToJsonList(path, r2, GradingResult.class);

        List<GradingResult> results = FileUtil.readJsonList(path, GradingResult.class);
        assertEquals(2, results.size(), "应该有2条结果记录");
        assertEquals("task-001", results.get(0).getTaskId());
        assertEquals("/output/review.docx", results.get(0).getWordPath());
        assertEquals("/output/scores.xlsx", results.get(0).getExcelPath());
        assertNull(results.get(1).getExcelPath(), "第二次批阅没有Excel，应为null");
        assertEquals(LocalDateTime.of(2026, 7, 14, 14, 35, 0), results.get(0).getExpireTime());
    }

    // ========== 4. StudentResult 批量写入 ==========

    @Test
    @DisplayName("scores.json 批量追加学生成绩，多次批阅不覆盖")
    void testBatchAppendStudentScores() throws IOException {
        String path = tempDir.resolve("scores.json").toString();

        // 第一次批阅：3个学生
        List<StudentResult> batch1 = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            StudentResult s = new StudentResult();
            s.setTaskId("task-001");
            s.setStudentId("2021000" + i);
            s.setStudentName("学生" + i);
            s.setRawScore(String.valueOf(80 + i));
            s.setAiComment("表现良好，代码规范");
            s.setStatus("APPROVED");
            batch1.add(s);
        }

        // 写入第一批
        List<StudentResult> existing = FileUtil.exists(path)
                ? FileUtil.readJsonList(path, StudentResult.class) : new ArrayList<>();
        existing.addAll(batch1);
        FileUtil.writeJsonList(path, existing);

        // 第二次批阅：2个学生
        List<StudentResult> batch2 = new ArrayList<>();
        for (int i = 4; i <= 5; i++) {
            StudentResult s = new StudentResult();
            s.setTaskId("task-002");
            s.setStudentId("2021000" + i);
            s.setStudentName("学生" + i);
            s.setRawScore(String.valueOf(70 + i));
            s.setAiComment("需要改进");
            s.setStatus("APPROVED");
            batch2.add(s);
        }

        // 写入第二批
        existing = FileUtil.exists(path)
                ? FileUtil.readJsonList(path, StudentResult.class) : new ArrayList<>();
        existing.addAll(batch2);
        FileUtil.writeJsonList(path, existing);

        // 验证总数
        List<StudentResult> all = FileUtil.readJsonList(path, StudentResult.class);
        assertEquals(5, all.size(), "两次批阅共5条成绩");

        // 验证按 taskId 过滤
        long task1Count = all.stream().filter(s -> "task-001".equals(s.getTaskId())).count();
        long task2Count = all.stream().filter(s -> "task-002".equals(s.getTaskId())).count();
        assertEquals(3, task1Count, "task-001 有3条");
        assertEquals(2, task2Count, "task-002 有2条");

        // 验证字段完整性
        StudentResult first = all.get(0);
        assertEquals("task-001", first.getTaskId());
        assertEquals("20210001", first.getStudentId());
        assertEquals("学生1", first.getStudentName());
        assertEquals("81", first.getRawScore());
        assertEquals("APPROVED", first.getStatus());
        assertNull(first.getTeacherScore(), "教师分数未设置应为null");
    }

    // ========== 5. 空文件处理 ==========

    @Test
    @DisplayName("文件不存在时 readJsonList 不应被调用，应走空列表分支")
    void testFileNotExistReturnsEmpty() {
        String path = tempDir.resolve("not_exist.json").toString();
        assertFalse(FileUtil.exists(path), "文件不应存在");
    }

    @Test
    @DisplayName("空列表写入后再追加，数据正确")
    void testEmptyListThenAppend() throws IOException {
        String path = tempDir.resolve("tasks_empty.json").toString();

        // 先写入空列表
        FileUtil.writeJsonList(path, new ArrayList<GradingTask>());
        List<GradingTask> empty = FileUtil.readJsonList(path, GradingTask.class);
        assertEquals(0, empty.size());

        // 再追加一条
        GradingTask t = new GradingTask();
        t.setId("task-after-empty");
        t.setTaskName("空列表后追加");
        t.setStatus(2);
        t.setCreateTime(LocalDateTime.now());
        appendToJsonList(path, t, GradingTask.class);

        List<GradingTask> after = FileUtil.readJsonList(path, GradingTask.class);
        assertEquals(1, after.size());
        assertEquals("task-after-empty", after.get(0).getId());
    }

    // ========== 6. 特殊字符和中文 ==========

    @Test
    @DisplayName("中文任务名和评语的序列化")
    void testChineseContentSerialization() throws IOException {
        String path = tempDir.resolve("chinese.json").toString();

        StudentResult s = new StudentResult();
        s.setTaskId("task-cn");
        s.setStudentId("20210099");
        s.setStudentName("张三丰");
        s.setRawScore("95");
        s.setAiComment("代码结构清晰，命名规范，注释完整。算法效率较高，时间复杂度为O(nlogn)。");
        s.setTeacherComment("同意AI评分，额外加分：课堂表现优秀");
        s.setTeacherNote("该生本学期进步明显");
        s.setStatus("APPROVED");
        s.setErrorMessage(null);

        FileUtil.writeJson(path, s);
        StudentResult loaded = FileUtil.readJson(path, StudentResult.class);

        assertEquals("张三丰", loaded.getStudentName());
        assertTrue(loaded.getAiComment().contains("时间复杂度为O(nlogn)"));
        assertTrue(loaded.getTeacherComment().contains("课堂表现优秀"));
        assertNull(loaded.getErrorMessage());
    }

    // ========== 7. GradingTask 和 GradingResult 关联 ==========

    @Test
    @DisplayName("GradingTask.id 与 GradingResult.taskId 关联查询")
    void testTaskResultLinkage() throws IOException {
        String taskPath = tempDir.resolve("tasks.json").toString();
        String resultPath = tempDir.resolve("results.json").toString();

        // 写入任务
        GradingTask task = new GradingTask();
        task.setId("link-task-001");
        task.setTaskName("关联测试");
        task.setTotalStudents(10);
        task.setGradedStudents(8);
        task.setStatus(2);
        task.setCreateTime(LocalDateTime.now());
        task.setFinishTime(LocalDateTime.now());
        appendToJsonList(taskPath, task, GradingTask.class);

        // 写入关联结果
        GradingResult result = new GradingResult();
        result.setTaskId("link-task-001");
        result.setStatus(0);
        result.setCreateTime(LocalDateTime.now());
        result.setWordPath("/out/review.docx");
        result.setExcelPath("/out/scores.xlsx");
        result.setExpireTime(LocalDateTime.now().plusDays(7));
        appendToJsonList(resultPath, result, GradingResult.class);

        // 模拟关联查询：通过 taskId 查找
        List<GradingTask> tasks = FileUtil.readJsonList(taskPath, GradingTask.class);
        List<GradingResult> results = FileUtil.readJsonList(resultPath, GradingResult.class);

        String searchId = "link-task-001";
        GradingTask foundTask = tasks.stream()
                .filter(t -> searchId.equals(t.getId()))
                .findFirst().orElse(null);
        GradingResult foundResult = results.stream()
                .filter(r -> searchId.equals(r.getTaskId()))
                .findFirst().orElse(null);

        assertNotNull(foundTask, "应找到任务");
        assertNotNull(foundResult, "应找到结果");
        assertEquals(foundTask.getId(), foundResult.getTaskId(), "taskId 应匹配");
        assertEquals(10, foundTask.getTotalStudents());
        assertEquals("/out/review.docx", foundResult.getWordPath());
    }
}
