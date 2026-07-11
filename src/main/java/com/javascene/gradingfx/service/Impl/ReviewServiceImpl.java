package com.javascene.gradingfx.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.javascene.gradingfx.config.bean.DifyClient;
import com.javascene.gradingfx.config.property.AppConfig;
import com.javascene.gradingfx.config.property.DataConfig;
import com.javascene.gradingfx.config.property.PythonConfig;
import com.javascene.gradingfx.constant.ErrorConstant;
import com.javascene.gradingfx.constant.ErrorCodeConstant;
import com.javascene.gradingfx.exception.BusinessException;
import com.javascene.gradingfx.exception.FilesNotFoundException;
import com.javascene.gradingfx.exception.ServerDocumentParsingException;
import com.javascene.gradingfx.model.*;
import com.javascene.gradingfx.service.ReviewService;
import com.javascene.gradingfx.util.ConfigLoader;
import com.javascene.gradingfx.util.FileUtil;
import com.javascene.gradingfx.util.ZipUtil;
import com.javascene.gradingfx.repository.StudentResultRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ReviewServiceImpl implements ReviewService {
    private final ObjectMapper mapper =  new ObjectMapper();

    private final String pythonExe;
    private final String pythonScriptPath;   // docx → md 脚本路径
    private final String mdToDocxScriptPath; // md → docx 脚本路径
    private final String tempWord;           // word 临时输出目录
    private final DifyClient difyClient;
    private final com.javascene.gradingfx.config.property.DifyConfig.ApiConfig difyProperty;
    private final String resultPath; // 导出文件根路径

    // ==================== 批阅引擎线程控制 ====================
    private final ReentrantLock pauseLock = new ReentrantLock();
    private final Condition pausedCondition = pauseLock.newCondition();
    private volatile boolean isPaused = false;
    private volatile boolean isCancelled = false;
    private volatile boolean isRunning = false;
    private volatile Thread schedulerThread;

    // ==================== 当前任务状态 ====================
    private volatile List<StudentResult> currentStudents;
    private volatile String currentTaskId;
    private volatile String currentRubric;
    private volatile int completedCount = 0;
    private volatile int failedCount = 0;
    private volatile ReviewService.ReviewProgressCallback currentCallback;
    /** 重试时保存完整学生列表，用于回调返回完整结果 */
    private volatile List<StudentResult> fullStudentList;

    // ==================== 依赖组件 ====================
    private final StudentResultRepository studentRepository = new StudentResultRepository();
    private final ExportServiceImpl exportService = new ExportServiceImpl();

    public ReviewServiceImpl() {
        AppConfig config = ConfigLoader.getConfig();
        PythonConfig python = config.getPython();
        this.pythonExe = python.getExe();
        this.pythonScriptPath = python.getScript().getDocxToMdPath();
        this.mdToDocxScriptPath = python.getScript().getMdToDocxPath();
        this.tempWord = config.getFile().getWordPath();
        this.difyClient = new DifyClient(config.getDify().getApi().getBaseUrl());
        this.difyProperty = config.getDify().getApi();
        this.resultPath = config.getFile().getResultPath() != null
                ? config.getFile().getResultPath()
                : "./run/exports";
    }

    //匹配学号和姓名
    private static final Pattern STUDENT_INFO_PATTERN = Pattern.compile("^(\\d{10})(\\s*)([\\u4e00-\\u9fa5·]{2,16})$");

    private String getTaskFilePath(String taskId) {
        DataConfig data = ConfigLoader.getConfig().getData();
        return data.getTask() + File.separator + "task_" + taskId + ".json";
    }

    private String getTotalTaskFilePath() {
        DataConfig data = ConfigLoader.getConfig().getData();
        return data.getTotalTask();
    }
    /**
     * 批量提取总压缩包内的所有学生作业信息
     * @param totalZipPath 总压缩包路径
     * @return 学生作业信息列表
     * @throws Exception 处理异常
     */
    public List<StudentHomework> extractFromTotalZip(String totalZipPath) throws Exception {
        Path tempRoot = Files.createTempDirectory("total_zip_" + UUID.randomUUID().toString());
        List<StudentHomework> results = new ArrayList<>();

        try {
            // 1. 解压总压缩包
            ZipUtil.unzip(totalZipPath, tempRoot.toFile());

            // 2. 遍历解压后的根目录，查找所有学生压缩包
            File[] files = tempRoot.toFile().listFiles();
            if (files == null) return results;

            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(".zip")) {
                    try {
                        StudentHomework homework = processStudentZip(file.getAbsolutePath());
                        if (homework != null) {
                            results.add(homework);
                        }
                    } catch (Exception e) {
                        log.error("处理学生压缩包失败: {}, 原因: {}", file.getName(), e.getMessage());
                    }
                }
            }
        } finally {
            // 清理临时目录
            FileUtils.deleteDirectory(tempRoot.toFile());
        }
        return results;
    }

    /**
     * 处理单个学生压缩包，提取学号、姓名和 Word 内容
     */
    private StudentHomework processStudentZip(String studentZipPath) throws Exception {
        Path tempStudentDir = Files.createTempDirectory("student_");
        try {
            // 解压学生压缩包
            ZipUtil.unzip(studentZipPath, tempStudentDir.toFile());

            // 从压缩包文件名解析学号、姓名
            String zipFileName = new File(studentZipPath).getName();
            String baseName = zipFileName.substring(0, zipFileName.lastIndexOf('.'));
            Matcher m = STUDENT_INFO_PATTERN.matcher(baseName);
            String studentId = null, studentName = null;
            if (m.find()) {
                studentId = m.group(1);
                studentName = m.group(3);
            } else {
                // 如果文件名不符合预期，尝试从解压后的文件夹名提取
                // 假设解压后只有一个文件夹，且命名为”学号姓名项目作业”
                File[] subFiles = tempStudentDir.toFile().listFiles(File::isDirectory);
                if (subFiles != null && subFiles.length == 1) {
                    String folderName = subFiles[0].getName();
                    Matcher folderMatcher = STUDENT_INFO_PATTERN.matcher(folderName);
                    if (folderMatcher.find()) {
                        studentId = folderMatcher.group(1);
                        studentName = folderMatcher.group(3);
                    }
                }
            }

            if (studentId == null || studentName == null) {
                log.error("无法识别学号姓名: " + zipFileName);
                return null;
            }

            // 查找 Word 文档（优先 .docx，其次 .doc）
            File wordFile = ZipUtil.findWordFile(tempStudentDir.toFile());
            if (wordFile == null) {
                log.error("未找到 Word 文档: " + studentZipPath);
                return null;
            }

            String wordContent = wordConvertToMd(List.of(wordFile.getAbsolutePath())).get("files").get(0);
            if (wordContent == null) {
                log.error("转换Word文档为Markdown格式失败: " + wordFile.getAbsolutePath());
                return null;
            }
            return new StudentHomework(studentId, studentName, wordContent);

        } finally {
            // 清理学生临时目录
            FileUtils.deleteDirectory(tempStudentDir.toFile());
        }
    }

    /**
     * 调用Python脚本转换Word文档为Markdown格式
     * @param fileUrls
     * @return
     */
    @Override
    public Map<String, List<String>> wordConvertToMd(List<String> fileUrls) {
        log.info("开始转换Word文档为Markdown格式");
        try {
            //结果集
            List<String> results = new ArrayList<>();

            for (String fileUrl : fileUrls) {
                List<String> command = new ArrayList<>();
                command.add(pythonExe);
                command.add(pythonScriptPath);
                command.add(fileUrl);

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                pb.environment().put("PYTHONIOENCODING", "utf-8");
                Process process = pb.start();
                log.info("开始执行Python脚本: {}", command);

                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                boolean finished = process.waitFor(120, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    throw new RuntimeException("Python脚本执行超时");
                }

                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    throw new RuntimeException("Python脚本退出代码" + exitCode + "\nOutput:\n" + output);
                }

                results.add(output.toString().trim());
            }
            log.info("转换完成，共转换 {} 个文件", results.size());
            return Map.of("files", results);


        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }


    @Override
    public Map<String, String>  mdConvertToWord(String difyResponseJson) {
        Path tempFile = null;
        // 校验输入参数
        if (null == difyResponseJson || difyResponseJson.isEmpty()) {
            throw new ServerDocumentParsingException(ErrorConstant.MD_CONTENT_EMPTY);
        }
        log.info(difyResponseJson);

        // 兼容两种输入格式：
        // 1. Dify API 响应（含 task_id）→ 用 extractOutputAsJson 提取
        // 2. 已提取好的 {"output": [...]} → 直接使用
        String outputJson;
        try {
            JsonNode checkNode = mapper.readTree(difyResponseJson);
            if (checkNode.has("output") && !checkNode.has("task_id")) {
                outputJson = difyResponseJson;
            } else {
                outputJson = extractOutputAsJson(difyResponseJson);
            }
        } catch (Exception e) {
            outputJson = extractOutputAsJson(difyResponseJson);
        }
        // 校验outputJson是否为空
        if(outputJson == null){
            throw new ServerDocumentParsingException(ErrorConstant.MD_CONTENT_INVALID);
        }


        try {
            //在系统默认临时目录下创建一个临时文件
            //前缀是 "dify_review_"，后缀是 ".json"
            tempFile = Files.createTempFile("dify_review_", ".json");

            // 将JSON 字符串以 UTF-8 编码写入这个临时文件
            Files.writeString(tempFile, outputJson, StandardCharsets.UTF_8);
            log.info("已成功创建本地临时中转文件: {}", tempFile.toAbsolutePath());

            // 构建命令行
            List<String> command = new ArrayList<>();
            command.add(pythonExe);
            command.add(mdToDocxScriptPath);
            command.add(tempFile.toAbsolutePath().toString()); // 传入路径
            command.add(tempWord); // 传入临时目录路径参数

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            log.info("开始调用 Python 脚本...");

            Process process = pb.start();
            String outputPath = "";
            // 读取 Python 控制台输出
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[Python输出] {}", line);
                    outputPath = line.trim();
                }
            }

            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Python md_to_docx 脚本执行超时");
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                String[] parts = outputPath.split(Pattern.quote("-"), 2);
                outputPath = parts.length > 1 ? parts[1].trim() : outputPath.trim();
                log.info("Word 文档已顺利生成。{}", outputPath);
                return Map.of("wordPath", outputPath);
            } else {
                log.error("Python 脚本执行失败，退出码: {}", exitCode);
                throw new RuntimeException("Python md_to_docx 脚本执行失败，退出码: " + exitCode);
            }

        } catch (Exception e) {
            throw  new RuntimeException(e);
        } finally {
            // 无论执行成功还是失败，最后都把硬盘上的临时文件删掉
            if (tempFile != null && Files.exists(tempFile)) {
                try {
                    Files.delete(tempFile);
                    log.info("临时中转文件已安全自动删除。");
                } catch (Exception e) {
                    log.error("清除临时文件失败: {}", e.getMessage());
                }
            }
        }
    }

    @Override
    public List<GradingTask> loadAllTasks() {
        String taskFilePath = getTotalTaskFilePath();
        try {
            return FileUtil.readJsonList(taskFilePath, GradingTask.class);
        } catch (IOException e) {
            log.error("读取任务文件失败: {}", e.getMessage());
            return List.of();
        }

    }

    @Override
    public List<StudentResultProperty> loadTask(String taskId) {
        List<StudentResult> results = studentRepository.loadByTaskId(taskId);
        List<StudentResultProperty> props = new ArrayList<>();
        for (StudentResult r : results) {
            props.add(toProperty(r));
        }
        return props;
    }

    private Map<String, Object> handleProject_zip(String outputJson) {
        Map<String, Object> result = new HashMap<>();
        List<String> reviews = new ArrayList<>();
        try{
            JsonNode root = mapper.readTree(outputJson);
            if (!root.has("output")) {
                throw new ServerDocumentParsingException(ErrorConstant.MD_CONTENT_INVALID);
            }
            JsonNode dataNode = root.get("output");
            if(dataNode.isArray()){
                for (JsonNode student : dataNode) {
                    String studentName = student.path("stu_name").asText("");
                    String studentId = student.path("stu_id").asText("");
                    String totalScore = student.path("total_score").asText("");
                    String review =  student.path("review").asText("");
                    if (studentId.isEmpty()) {
                        log.warn("跳过缺少学号的学生记录");
                        continue;
                    }
                    // key = 学生学号，value = list（name,totalScore）用于生成execl文件
                    result.put(studentId, List.of(studentName, totalScore));
                    // 存储review，用于生成word文档
                    reviews.add(review);
                }
                result.put("reviews", reviews);
            }
            return result;
        } catch (ServerDocumentParsingException e) {
            throw e;
        } catch (Exception e) {
            log.error("JSON 解析失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从Dify工作流响应中提取output字段
     * @param difyResponseJson
     * @return
     */
    public String extractOutputAsJson(String difyResponseJson) {

        try {
            JsonNode root = mapper.readTree(difyResponseJson);

            if (!root.has("task_id")) {
                log.error("JSON 中未找到 'task_id' 键");
                return null;
            }

            JsonNode dataNode = root.get("data");
            if (dataNode == null || !dataNode.has("status")) {
                log.error("JSON 中未找到 'status' 键");
                return null;
            }

            String status = dataNode.get("status").asText();
            if (!"succeeded".equals(status)) {
                log.error("Dify 工作流调用失败，状态为：{}", status);
                return null;
            }

            JsonNode outputsNode = dataNode.get("outputs");
            if (outputsNode == null || !outputsNode.has("output")) {
                // 打印实际返回的 keys 帮助排查
                log.error("JSON 中未找到 'output' 键, 实际 outputs 内容: {}", outputsNode);
                // 兼容：遍历 outputs 找第一个 Array 类型的值
                if (outputsNode != null) {
                    Iterator<Map.Entry<String, JsonNode>> fields = outputsNode.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> entry = fields.next();
                        if (entry.getValue().isArray()) {
                            log.info("使用替代输出键: {}", entry.getKey());
                            JsonNode outputNode = entry.getValue();
                            ObjectNode resultJson = mapper.createObjectNode();
                            resultJson.set("output", outputNode);
                            return mapper.writeValueAsString(resultJson);
                        }
                    }
                }
                return null;
            }

            JsonNode outputNode = outputsNode.get("output");
            ObjectNode resultJson = mapper.createObjectNode();
            resultJson.set("output", outputNode);
            return mapper.writeValueAsString(resultJson);

        } catch (Exception e) {
            log.error("JSON 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 调用Dify工作流
     * @param fileUrls 文件路径列表
     * @param rubric 评分标准
     * @return 完整拼接后的字符串（包含 Dify 吐出来的所有块）
     */

    public String runWorkflowWithCommonFiles(List<String> fileUrls, String rubric) {
        // 检查文件路径列表是否为空
        if (fileUrls.isEmpty()) {
            throw new FilesNotFoundException(ErrorConstant.FILES_NOT_FOUND);}
        // 先转换为Markdown格式
        Map<String, List<String>> mdFiles = wordConvertToMd(fileUrls);
        // 调用Dify工作流
        Map<String, Object> requestBody  = new HashMap<>();
        if (null != rubric && !rubric.isEmpty()) {
            requestBody.put("rubric", rubric);
        }
        requestBody.put("upload_filesOFmd", mdFiles);
        requestBody.put("handle_type", "common_files");
        //工作流返回的Markdown字符串，转换为Word文档
        try {
            String result = difyClient.runWorkflowBlocking(difyProperty.getApiKey(), requestBody);
            return mdConvertToWord(result).get("wordPath");
        } catch (ServerDocumentParsingException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 生成Excel文档
     * TODO
     * @param json
     * @return
     */
    private Map<String, String> generateExcel(Map<String, Object> json) {
        log.warn("Excel 生成功能尚未实现，跳过 Excel 导出");
        return new HashMap<>();
    }

    /**
     * 向 JSON 列表文件追加一个对象：读出 list → add → 写回
     */
    private static final Object FILE_LOCK = new Object();

    private <T> void appendToJsonList(String filePath, T element, Class<T> clazz) throws IOException {
        synchronized (FILE_LOCK) {
            List<T> list;
            if (FileUtil.exists(filePath) && new File(filePath).length() > 0) {
                list = FileUtil.readJsonList(filePath, clazz);
            } else {
                list = new ArrayList<>();
            }
            list.add(element);
            FileUtil.writeJson(filePath, list);
        }
    }



    @Override
    public String startBatchReviewFromZip(String zipFilePath, String rubric, ReviewService.ReviewProgressCallback callback) {
        String taskId = UUID.randomUUID().toString();

        GradingTask task = new GradingTask();
        task.setId(taskId);
        task.setFilePath(zipFilePath);
        task.setStatus(GradingTask.STATUS_PROCESSING);
        task.setCreateTime(LocalDateTime.now());
        String zipName = new File(zipFilePath).getName();
        task.setTaskName(zipName.endsWith(".zip") ? zipName.substring(0, zipName.length() - 4) : zipName);

        try {
            List<StudentHomework> homeworks = extractFromTotalZip(zipFilePath);
            task.setTotalStudents(homeworks.size());

            if (homeworks.isEmpty()) {
                throw new BusinessException(ErrorCodeConstant.FILE_NOT_FOUND, ErrorConstant.FILES_NOT_FOUND);
            }

            List<StudentResult> students = new ArrayList<>();
            for (StudentHomework hw : homeworks) {
                students.add(toStudentResult(hw, taskId));
            }

            String taskFile = ConfigLoader.getConfig().getData().getTotalTask();
            ensureParentDirExists(taskFile);
            appendToJsonList(taskFile, task, GradingTask.class);

            // 创建 GradingResult 并写入 results.json
            GradingResult gradingResult = new GradingResult();
            gradingResult.setTaskId(taskId);
            gradingResult.setStatus(1); // 处理中
            gradingResult.setCreateTime(LocalDateTime.now());
            gradingResult.setScoreChanged(0);
            try {
                String resultsPath = getResultsJsonPath();
                ensureParentDirExists(resultsPath);
                appendToJsonList(resultsPath, gradingResult, GradingResult.class);
            } catch (IOException e) {
                log.error("写入 GradingResult 失败: {}", e.getMessage());
            }

            startBatchReview(students, taskId, rubric, callback);
            return taskId;

        } catch (Exception e) {
            log.error("启动批阅任务失败: {}", e.getMessage(), e);
            task.setStatus(GradingTask.STATUS_FAILED);
            task.setErrorMessage(e.getMessage());
            task.setFinishTime(LocalDateTime.now());
            try {
                String taskFile = ConfigLoader.getConfig().getData().getTotalTask();
                ensureParentDirExists(taskFile);
                appendToJsonList(taskFile, task, GradingTask.class);
            } catch (Exception ex) {
                log.error("保存失败任务异常: {}", ex.getMessage());
            }
            if (callback != null) {
                callback.onReviewError(e.getMessage());
            }
            throw new BusinessException(ErrorCodeConstant.UNKNOWN_ERROR, ErrorConstant.UNKNOWN_ERROR);
        }
    }

    @Override
    public void startBatchReview(List<StudentResult> allStudents, String taskId, String rubric, ReviewService.ReviewProgressCallback callback) {
        if (isRunning) {
            log.warn("批阅任务正在运行中，无法启动新任务");
            if (callback != null) {
                callback.onReviewError("批阅任务正在运行中，请先停止当前任务");
            }
            return;
        }

        this.currentStudents = new ArrayList<>(allStudents);
        this.currentTaskId = taskId;
        this.currentRubric = rubric;
        this.currentCallback = callback;
        this.fullStudentList = null;
        this.completedCount = 0;
        this.failedCount = 0;
        this.isPaused = false;
        this.isCancelled = false;
        this.isRunning = true;

        schedulerThread = new Thread(this::runBatchReviewScheduler, "BatchReview-Scheduler");
        schedulerThread.setDaemon(true);
        schedulerThread.start();
    }

    /**
     * 批阅调度线程主逻辑：循环处理批次，每批 5 个学生一次性调用 Dify（并行在 Dify 端）
     */
    private void runBatchReviewScheduler() {
        int totalStudents = currentStudents.size();
        int totalBatches = (totalStudents + 4) / 5;
        log.info("开始批阅: taskId={}, 总学生数={}, 总批次数={}", currentTaskId, totalStudents, totalBatches);

        try {
            for (int batchIdx = 0; batchIdx < totalBatches; batchIdx++) {
                // ===== 暂停检查点 =====
                pauseLock.lock();
                try {
                    while (isPaused && !isCancelled) {
                        log.info("批阅已暂停，等待继续... (当前批次 {}/{})", batchIdx, totalBatches);
                        pausedCondition.await();
                    }
                } finally {
                    pauseLock.unlock();
                }

                // ===== 取消检查 =====
                if (isCancelled) {
                    log.info("批阅已被取消，停止调度 (已完成 {} 批)", batchIdx);
                    break;
                }

                // ===== 获取当前批次学生 =====
                int from = batchIdx * 5;
                int to = Math.min(from + 5, totalStudents);
                List<StudentResult> batch = new ArrayList<>(currentStudents.subList(from, to));

                // 记录批阅前已完成的学生（重试场景防重复计数）
                Set<String> alreadyApproved = new HashSet<>();
                for (StudentResult s : batch) {
                    if (StudentResult.STATUS_APPROVED.equals(s.getStatus())) {
                        alreadyApproved.add(s.getStudentId());
                    }
                }

                // 标记为处理中（已完成的跳过——重试场景）
                for (StudentResult s : batch) {
                    if (!StudentResult.STATUS_APPROVED.equals(s.getStatus())) {
                        s.setStatus(StudentResult.STATUS_PROCESSING);
                    }
                }

                log.info("开始处理第 {}/{} 批 ({} 个学生)", batchIdx + 1, totalBatches, batch.size());

                // ===== 一次性将整批作业发送给 Dify（并行在 Dify 端） =====
                callDifyForBatch(batch, currentRubric);

                // ===== 统计结果（跳过批阅前已完成的学生） =====
                for (StudentResult s : batch) {
                    if (alreadyApproved.contains(s.getStudentId())) continue;
                    if (StudentResult.STATUS_APPROVED.equals(s.getStatus())) {
                        completedCount++;
                    } else if (!StudentResult.STATUS_PENDING.equals(s.getStatus())) {
                        failedCount++;
                    }
                }

                log.info("第 {}/{} 批完成 (累计已完成: {}, 失败: {})", batchIdx + 1, totalBatches, completedCount, failedCount);

                // ===== 持久化（只传当前批次，让合并逻辑有意义） =====
                saveProgress(batch, currentTaskId);

                // ===== 重试场景：将本批结果合并回完整列表 =====
                if (fullStudentList != null) {
                    Map<String, StudentResult> batchMap = new LinkedHashMap<>();
                    for (StudentResult s : batch) {
                        batchMap.put(s.getStudentId(), s);
                    }
                    for (int i = 0; i < fullStudentList.size(); i++) {
                        StudentResult updated = batchMap.get(fullStudentList.get(i).getStudentId());
                        if (updated != null) {
                            fullStudentList.set(i, updated);
                        }
                    }
                }

                // ===== UI 回调 =====
                if (currentCallback != null) {
                    currentCallback.onBatchCompleted(completedCount, failedCount, totalStudents, batch);
                }
            }

            // ===== 全部完成 =====
            log.info("批阅全部完成: taskId={}, 已完成={}, 失败={}", currentTaskId, completedCount, failedCount);
            updateGradingTaskStatus(currentTaskId, GradingTask.STATUS_SUCCESS);
            // 更新 GradingResult 状态为成功
            GradingResult gr = findGradingResult(currentTaskId);
            if (gr != null) {
                gr.setStatus(0);
                gr.setExpireTime(LocalDateTime.now().plusDays(7));
                updateGradingResult(gr);
            }
            if (currentCallback != null) {
                List<StudentResult> finalResults = fullStudentList != null
                        ? new ArrayList<>(fullStudentList)
                        : new ArrayList<>(currentStudents);
                fullStudentList = null;
                currentCallback.onReviewFinished(finalResults);
            }

        } catch (InterruptedException e) {
            log.warn("批阅调度线程被中断，保存已批阅结果");
            Thread.currentThread().interrupt();
            saveProgress(currentStudents, currentTaskId);
            updateGradingTaskStatus(currentTaskId, GradingTask.STATUS_FAILED);
            if (currentCallback != null) {
                currentCallback.onReviewError("批阅被中断，已保存已批阅结果");
            }
        } catch (Exception e) {
            log.error("批阅调度异常: {}", e.getMessage(), e);
            saveProgress(currentStudents, currentTaskId);
            updateGradingTaskStatus(currentTaskId, GradingTask.STATUS_FAILED);
            if (currentCallback != null) {
                currentCallback.onReviewError("批阅异常: " + e.getMessage());
            }
        } finally {
            isRunning = false;
        }
    }

    /**
     * 一次性调用 Dify 批阅整批学生作业（并行在 Dify 端）
     * @param batch 当前批次的学生列表（最多 5 个）
     * @param rubric 评分标准
     */
    private void callDifyForBatch(List<StudentResult> batch, String rubric) {
        // 构建 homework 列表（跳过已完成的——重试场景）
        List<StudentHomework> homeworks = new ArrayList<>();
        Map<String, StudentResult> studentMap = new LinkedHashMap<>();
        for (StudentResult s : batch) {
            if (StudentResult.STATUS_APPROVED.equals(s.getStatus())) {
                log.info("跳过已批阅学生: {} ({})", s.getStudentName(), s.getStudentId());
                continue;
            }
            StudentHomework hw = new StudentHomework(
                    s.getStudentId(),
                    s.getStudentName(),
                    s.getWordContent()
            );
            homeworks.add(hw);
            studentMap.put(s.getStudentId(), s);
        }

        if (homeworks.isEmpty()) {
            log.info("本批次所有学生已完成，跳过 Dify 调用");
            return;
        }

        log.info("调用 Dify 批阅 {} 个学生", homeworks.size());

        try {
            // 构建 inputs（DifyClient.buildRequest 会自动包装 inputs/user/response_mode）
            Map<String, Object> inputs = new HashMap<>();
            if (rubric != null && !rubric.isEmpty()) {
                inputs.put("rubric", rubric);
            }
            // Dify 要求 upload_filesOFmd 为 dict 类型，包装为 {"files": [...]}
            Map<String, Object> homeworksWrapper = new HashMap<>();
            homeworksWrapper.put("files", homeworks);
            inputs.put("upload_filesOFmd", homeworksWrapper);
            inputs.put("handle_type", "project_zip");

            // 调用 Dify 工作流（阻塞等待返回）
            String response = difyClient.runWorkflowBlocking(difyProperty.getApiKey(), inputs);

            // 提取 output JSON
            String outputJson = extractOutputAsJson(response);
            if (outputJson == null) {
                for (StudentResult s : studentMap.values()) {
                    s.setStatus(StudentResult.STATUS_FAILED);
                    s.setErrorMessage("Dify返回结果解析失败");
                }
                return;
            }

            // 解析 output 数组，按 stu_id 映射回 StudentResult
            JsonNode root = mapper.readTree(outputJson);
            JsonNode outputArray = root.get("output");
            if (outputArray == null || !outputArray.isArray()) {
                for (StudentResult s : studentMap.values()) {
                    s.setStatus(StudentResult.STATUS_FAILED);
                    s.setErrorMessage("Dify输出格式异常: output 不是数组");
                }
                return;
            }

            // 遍历 Dify 返回的学生结果数组
            Set<String> respondedIds = new HashSet<>();
            for (JsonNode studentNode : outputArray) {
                String stuId = studentNode.path("stu_id").asText("");
                String stuName = studentNode.path("stu_name").asText("");
                String totalScore = studentNode.path("total_score").asText("");
                String review = studentNode.path("content").asText("");

                if (stuId.isEmpty()) {
                    log.warn("跳过缺少学号的返回记录");
                    continue;
                }

                StudentResult sr = studentMap.get(stuId);
                if (sr == null) {
                    log.warn("Dify 返回的学号 {} 在当前批次中找不到对应学生", stuId);
                    continue;
                }

                sr.setStudentName(stuName.isEmpty() ? sr.getStudentName() : stuName);
                sr.setRawScore(totalScore);
                sr.setAiComment(review);
                sr.setStatus(StudentResult.STATUS_APPROVED);
                sr.setErrorMessage(null);
                respondedIds.add(stuId);
                log.info("学生 {} 批阅完成: 得分={}", stuId, totalScore);
            }

            // 未出现在响应中的学生标记为失败
            for (StudentResult s : studentMap.values()) {
                if (!respondedIds.contains(s.getStudentId())) {
                    s.setStatus(StudentResult.STATUS_FAILED);
                    s.setErrorMessage("Dify 未返回该学生的批阅结果");
                    log.warn("学生 {} 未在 Dify 响应中找到", s.getStudentId());
                }
            }

        } catch (Exception e) {
            log.error("批次 Dify 调用失败: {}", e.getMessage(), e);
            for (StudentResult s : studentMap.values()) {
                s.setStatus(StudentResult.STATUS_FAILED);
                s.setErrorMessage(e.getMessage());
            }
        }
    }

    /**
     * 持久化批阅结果：JSON + Word + TXT（Excel 改为按钮触发，不在此自动生成）
     * 合并策略：将当前批次结果与已有 JSON 合并（支持重试场景）
     */
    private void saveProgress(List<StudentResult> batchStudents, String taskId) {
        log.info("保存批阅进度: taskId={}, 当前批学生数={}", taskId, batchStudents.size());

        // 合并：加载已有 JSON，用当前批次结果替换对应学生
        List<StudentResult> existing = studentRepository.loadByTaskId(taskId);
        Map<String, StudentResult> batchMap = new LinkedHashMap<>();
        for (StudentResult s : batchStudents) {
            batchMap.put(s.getStudentId(), s);
        }

        List<StudentResult> merged = new ArrayList<>();
        for (StudentResult s : existing) {
            StudentResult updated = batchMap.get(s.getStudentId());
            if (updated != null) {
                merged.add(updated);
                batchMap.remove(s.getStudentId());
            } else {
                merged.add(s);
            }
        }
        merged.addAll(batchMap.values());

        // 保存 JSON（双文件：轻量主文件 + content 文件）
        studentRepository.saveByTaskId(taskId, merged);

        // 生成 Word
        String dir = getResultDir(taskId);
        try {
            FileUtil.ensureDirExists(dir);
        } catch (IOException e) {
            log.error("创建结果目录失败: {}", e.getMessage());
        }
        try {
            List<String> reviews = new ArrayList<>();
            for (StudentResult s : merged) {
                if (s.getAiComment() != null && !s.getAiComment().isEmpty()) {
                    reviews.add(s.getAiComment());
                }
            }
            if (!reviews.isEmpty()) {
                // 构造 output 数组节点
                JsonNode outputNode = mapper.valueToTree(reviews);
                ObjectNode resultJson = mapper.createObjectNode();
                resultJson.set("output", outputNode);
                String wordJsonStr = mapper.writeValueAsString(resultJson);
                // 调用 mdConvertToWord 生成 Word 文档
                String wordPath = mdConvertToWord(wordJsonStr).get("wordPath");
                // 将生成的 Word 文件复制到结果目录
                if (wordPath != null && !wordPath.isEmpty()) {
                    File srcWord = new File(wordPath);
                    File dstWord = new File(dir + File.separator + "summary.docx");
                    FileUtils.copyFile(srcWord, dstWord);
                    log.info("Word 文档已生成: {}", dstWord.getAbsolutePath());
                    // 将 Word 路径写入 GradingResult
                    updateGradingResultWordPath(taskId, dstWord.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            log.error("生成 Word 文档失败: {}", e.getMessage(), e);
        }

        // 4. 为本批次每个学生生成 TXT（覆盖）
        for (StudentResult s : batchStudents) {
            if (StudentResult.STATUS_APPROVED.equals(s.getStatus())
                    || StudentResult.STATUS_FAILED.equals(s.getStatus())) {
                exportService.exportTxt(s, dir + File.separator + s.getStudentId() + ".txt");
            }
        }
    }

    @Override
    public void pauseReview() {
        pauseLock.lock();
        try {
            isPaused = true;
            log.info("批阅已暂停（当前批次完成后将不再提交新批次）");
        } finally {
            pauseLock.unlock();
        }
    }

    @Override
    public void resumeReview() {
        pauseLock.lock();
        try {
            isPaused = false;
            pausedCondition.signalAll();
            log.info("批阅已继续");
        } finally {
            pauseLock.unlock();
        }
    }

    @Override
    public void stopReview() {
        isCancelled = true;
        pauseLock.lock();
        try {
            isPaused = false;
            pausedCondition.signalAll();
        } finally {
            pauseLock.unlock();
        }
        if (schedulerThread != null) {
            schedulerThread.interrupt();
        }
        log.info("批阅已请求停止，正在保存已批阅结果...");
    }

    @Override
    public void retryFailed(String taskId, String rubric, ReviewService.ReviewProgressCallback callback) {
        if (isRunning) {
            if (callback != null) {
                callback.onReviewError("批阅任务正在运行中，无法重试");
            }
            return;
        }

        // 重试需要 wordContent 重新调用 Dify，所以用 loadByTaskIdWithContent
        List<StudentResult> allStudents = studentRepository.loadByTaskIdWithContent(taskId);
        if (allStudents.isEmpty()) {
            if (callback != null) {
                callback.onReviewError("未找到任务数据: " + taskId);
            }
            return;
        }

        // 筛选 PENDING / FAILED / PROCESSING 的学生
        List<StudentResult> toRetry = new ArrayList<>();
        for (StudentResult s : allStudents) {
            if (!StudentResult.STATUS_APPROVED.equals(s.getStatus())) {
                s.setStatus(StudentResult.STATUS_PENDING);
                s.setRawScore(null);
                s.setAiComment(null);
                s.setErrorMessage(null);
                toRetry.add(s);
            }
        }

        if (toRetry.isEmpty()) {
            log.info("没有需要重试的学生 (taskId={})", taskId);
            if (callback != null) {
                callback.onReviewFinished(allStudents);
            }
            return;
        }

        log.info("重试 {} 个学生 (taskId={})", toRetry.size(), taskId);
        this.fullStudentList = new ArrayList<>(allStudents);
        startBatchReview(toRetry, taskId, rubric, callback);
    }

    @Override
    public boolean isReviewRunning() {
        return isRunning;
    }

    @Override
    public String exportExcel(String taskId) {
        String dir = getResultDir(taskId);
        try {
            FileUtil.ensureDirExists(dir);
        } catch (IOException e) {
            log.error("创建结果目录失败: {}", e.getMessage());
        }
        String excelPath = dir + File.separator + "summary.xlsx";

        // 从 results.json 读取 GradingResult，判断 scoreChanged
        GradingResult gr = findGradingResult(taskId);
        if (gr != null && gr.getScoreChanged() == 0
                && gr.getExcelPath() != null && !gr.getExcelPath().isEmpty()
                && new File(gr.getExcelPath()).exists()) {
            // 评分未变更且 Excel 已存在，直接返回路径
            log.info("评分未变更，直接使用已生成的 Excel: {}", gr.getExcelPath());
            return copyToExportPath(gr.getExcelPath(), taskId, "xlsx");
        }

        // 评分已变更或 Excel 尚未生成，重新生成
        List<StudentResult> students = studentRepository.loadByTaskId(taskId);
        if (students.isEmpty()) {
            log.warn("导出 Excel 失败：未找到任务数据 taskId={}", taskId);
            return null;
        }
        exportService.exportExcel(students, excelPath);
        log.info("Excel 已重新生成: {} (共 {} 条)", excelPath, students.size());

        // 更新 GradingResult：写入 excelPath，重置 scoreChanged
        if (gr != null) {
            gr.setExcelPath(excelPath);
            gr.setScoreChanged(0);
            updateGradingResult(gr);
        }
        return copyToExportPath(excelPath, taskId, "xlsx");
    }

    @Override
    public void markScoreChanged(String taskId) {
        GradingResult gr = findGradingResult(taskId);
        if (gr != null) {
            gr.setScoreChanged(1);
            updateGradingResult(gr);
            log.info("已标记评分变更: taskId={}", taskId);
        } else {
            log.warn("未找到 GradingResult，无法标记评分变更: taskId={}", taskId);
        }
    }

    @Override
    public String exportWord(String taskId) {
        // 优先返回已生成的 Word 路径
        GradingResult gr = findGradingResult(taskId);
        if (gr != null && gr.getWordPath() != null && !gr.getWordPath().isEmpty()
                && new File(gr.getWordPath()).exists()) {
            log.info("Word 已存在，直接返回: {}", gr.getWordPath());
            return copyToExportPath(gr.getWordPath(), taskId, "docx");
        }

        // Word 尚未生成或文件丢失，重新生成
        String dir = getResultDir(taskId);
        try {
            FileUtil.ensureDirExists(dir);
        } catch (IOException e) {
            log.error("创建结果目录失败: {}", e.getMessage());
        }

        List<StudentResult> students = studentRepository.loadByTaskId(taskId);
        if (students.isEmpty()) {
            log.warn("导出 Word 失败：未找到任务数据 taskId={}", taskId);
            return null;
        }

        try {
            List<String> reviews = new ArrayList<>();
            for (StudentResult s : students) {
                if (s.getAiComment() != null && !s.getAiComment().isEmpty()) {
                    reviews.add(s.getAiComment());
                }
            }
            if (reviews.isEmpty()) {
                log.warn("没有可用的批语用于生成 Word");
                return null;
            }
            JsonNode outputNode = mapper.valueToTree(reviews);
            ObjectNode resultJson = mapper.createObjectNode();
            resultJson.set("output", outputNode);
            String wordJsonStr = mapper.writeValueAsString(resultJson);
            String wordPath = mdConvertToWord(wordJsonStr).get("wordPath");
            if (wordPath != null && !wordPath.isEmpty()) {
                File srcWord = new File(wordPath);
                File dstWord = new File(dir + File.separator + "summary.docx");
                FileUtils.copyFile(srcWord, dstWord);
                log.info("Word 文档已重新生成: {}", dstWord.getAbsolutePath());
                // 更新 GradingResult 的 wordPath
                if (gr != null) {
                    gr.setWordPath(dstWord.getAbsolutePath());
                    updateGradingResult(gr);
                }
                return copyToExportPath(dstWord.getAbsolutePath(), taskId, "docx");
            }
        } catch (Exception e) {
            log.error("重新生成 Word 文档失败: {}", e.getMessage(), e);
        }
        return null;
    }

    // ==================== 辅助方法 ====================

    /**
     * 确保文件路径的父目录存在（避免误将文件路径当目录创建）
     */
    private void ensureParentDirExists(String filePath) throws IOException {
        File parent = new File(filePath).getParentFile();
        if (parent != null) {
            FileUtil.ensureDirExists(parent.getAbsolutePath());
        }
    }

    private StudentResult toStudentResult(StudentHomework hw, String taskId) {
        StudentResult sr = new StudentResult();
        sr.setTaskId(taskId);
        sr.setStudentId(hw.getStudentId());
        sr.setStudentName(hw.getStudentName());
        sr.setWordContent(hw.getWordContent());
        sr.setStatus(StudentResult.STATUS_PENDING);
        return sr;
    }

    private StudentResultProperty toProperty(StudentResult sr) {
        return new StudentResultProperty(
                sr.getTaskId(), sr.getStudentId(), sr.getStudentName(),
                sr.getRawScore(), sr.getAiComment(),
                sr.getTeacherScore(), sr.getTeacherComment(), sr.getTeacherNote(),
                sr.getStatus(), sr.getErrorMessage());
    }

    private String getResultDir(String taskId) {
        return resultPath + File.separator + taskId;
    }

    // ==================== 导出路径配置 ====================

    /**
     * 加载导出路径配置
     */
    private ExportConfig loadExportConfig() {
        try {
            String configPath = ConfigLoader.getConfig().getData().getExportConfig();
            if (configPath == null || !FileUtil.exists(configPath)) return null;
            return FileUtil.readJson(configPath, ExportConfig.class);
        } catch (IOException e) {
            log.error("读取导出配置失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 totalTask.json 中按 taskId 获取任务名称
     */
    private String getTaskName(String taskId) {
        try {
            String taskFilePath = getTotalTaskFilePath();
            if (FileUtil.exists(taskFilePath)) {
                List<GradingTask> tasks = FileUtil.readJsonList(taskFilePath, GradingTask.class);
                for (GradingTask t : tasks) {
                    if (taskId.equals(t.getId())) {
                        return t.getTaskName();
                    }
                }
            }
        } catch (IOException e) {
            log.error("读取任务名称失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 根据文件名模板构建导出文件名
     * 支持变量: {date} 日期, {taskName} 任务名称
     */
    private String buildFileName(String template, String taskId, String extension) {
        String name = (template != null && !template.isEmpty()) ? template : "summary";
        // 替换变量
        name = name.replace("{date}", LocalDate.now().toString());
        String taskName = getTaskName(taskId);
        name = name.replace("{taskName}", taskName != null ? taskName : "未命名");
        // 确保扩展名
        if (!name.endsWith("." + extension)) {
            name = name + "." + extension;
        }
        // 文件名安全处理：替换非法字符
        name = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        return name;
    }

    /**
     * 将文件从系统路径复制到用户配置的导出路径
     * 如果未配置导出路径，直接返回源路径
     */
    private String copyToExportPath(String sourcePath, String taskId, String extension) {
        if (sourcePath == null || sourcePath.isEmpty()) return sourcePath;

        ExportConfig config = loadExportConfig();
        if (config == null || config.getOutputPath() == null || config.getOutputPath().isEmpty()) {
            // 未配置导出路径，返回系统路径
            return sourcePath;
        }

        String fileName = buildFileName(config.getFileNameTemplate(), taskId, extension);
        String destPath = config.getOutputPath() + File.separator + fileName;

        try {
            FileUtil.ensureDirExists(config.getOutputPath());
            FileUtils.copyFile(new File(sourcePath), new File(destPath));
            log.info("文件已复制到导出路径: {}", destPath);
            return destPath;
        } catch (IOException e) {
            log.error("复制到导出路径失败: {}", e.getMessage());
            return sourcePath;
        }
    }

    // ==================== GradingResult 管理 ====================

    private String getResultsJsonPath() {
        return ConfigLoader.getConfig().getData().getResult();
    }

    /**
     * 从 results.json 中按 taskId 查找 GradingResult
     */
    private GradingResult findGradingResult(String taskId) {
        synchronized (FILE_LOCK) {
            try {
                String resultsPath = getResultsJsonPath();
                if (!FileUtil.exists(resultsPath)) return null;
                List<GradingResult> results = FileUtil.readJsonList(resultsPath, GradingResult.class);
                for (GradingResult r : results) {
                    if (taskId.equals(r.getTaskId())) {
                        return r;
                    }
                }
            } catch (IOException e) {
                log.error("读取 GradingResult 失败: {}", e.getMessage());
            }
            return null;
        }
    }

    /**
     * 更新 results.json 中的 GradingResult（按 taskId 匹配后整体替换）
     */
    private void updateGradingResult(GradingResult updated) {
        synchronized (FILE_LOCK) {
            try {
                String resultsPath = getResultsJsonPath();
                if (!FileUtil.exists(resultsPath)) return;
                List<GradingResult> results = FileUtil.readJsonList(resultsPath, GradingResult.class);
                for (int i = 0; i < results.size(); i++) {
                    if (updated.getTaskId().equals(results.get(i).getTaskId())) {
                        results.set(i, updated);
                        break;
                    }
                }
                FileUtil.writeJsonList(resultsPath, results);
            } catch (IOException e) {
                log.error("更新 GradingResult 失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 仅更新 GradingResult 的 wordPath 字段
     */
    private void updateGradingResultWordPath(String taskId, String wordPath) {
        synchronized (FILE_LOCK) {
            try {
                String resultsPath = getResultsJsonPath();
                if (!FileUtil.exists(resultsPath)) return;
                List<GradingResult> results = FileUtil.readJsonList(resultsPath, GradingResult.class);
                for (GradingResult r : results) {
                    if (taskId.equals(r.getTaskId())) {
                        r.setWordPath(wordPath);
                        break;
                    }
                }
                FileUtil.writeJsonList(resultsPath, results);
            } catch (IOException e) {
                log.error("更新 GradingResult wordPath 失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 更新 GradingTask 状态（在 totalTask.json 中查找并更新）
     */
    private void updateGradingTaskStatus(String taskId, int status) {
        synchronized (FILE_LOCK) {
            try {
                String taskFilePath = getTotalTaskFilePath();
                List<GradingTask> tasks = FileUtil.exists(taskFilePath)
                        ? FileUtil.readJsonList(taskFilePath, GradingTask.class)
                        : new ArrayList<>();
                for (GradingTask t : tasks) {
                    if (t.getId() != null && t.getId().equals(taskId)) {
                        t.setStatus(status);
                        t.setGradedStudents(completedCount);
                        t.setFinishTime(LocalDateTime.now());
                        if (status == GradingTask.STATUS_FAILED) {
                            t.setErrorMessage("批阅中断或异常，已保存已批阅结果");
                        }
                        t.setRetryCount(t.getRetryCount() + 1);
                        break;
                    }
                }
                FileUtil.writeJsonList(taskFilePath, tasks);
            } catch (IOException e) {
                log.error("更新任务状态失败: {}", e.getMessage());
            }
        }
    }

}
