package com.javascene.gradingfx.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.javascene.gradingfx.config.bean.DifyClient;
import com.javascene.gradingfx.config.property.AppConfig;
import com.javascene.gradingfx.config.property.DataConfig;
import com.javascene.gradingfx.config.property.FileConfig;
import com.javascene.gradingfx.config.property.PythonConfig;
import com.javascene.gradingfx.constant.ErrorConstant;
import com.javascene.gradingfx.constant.ErrorCodeConstant;
import com.javascene.gradingfx.exception.BusinessException;
import com.javascene.gradingfx.exception.FilesNotFoundException;
import com.javascene.gradingfx.exception.ServerDocumentParsingException;
import com.javascene.gradingfx.model.GradingResult;
import com.javascene.gradingfx.model.GradingTask;
import com.javascene.gradingfx.model.StudentHomework;
import com.javascene.gradingfx.service.ReviewService;
import com.javascene.gradingfx.util.ConfigLoader;
import com.javascene.gradingfx.util.FileUtil;
import com.javascene.gradingfx.util.ZipUtil;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
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

    public ReviewServiceImpl() {
        AppConfig config = ConfigLoader.getConfig();
        PythonConfig python = config.getPython();
        this.pythonExe = python.getExe();
        this.pythonScriptPath = python.getScript().getDocxToMdPath();
        this.mdToDocxScriptPath = python.getScript().getMdToDocxPath();
        this.tempWord = config.getFile().getWordPath();
        this.difyClient = new DifyClient(config.getDify().getApi().getBaseUrl());
        this.difyProperty = config.getDify().getApi();
    }

    //匹配学号和姓名
    private static final Pattern STUDENT_INFO_PATTERN = Pattern.compile("^(\\d{10})(\\s*)([\\u4e00-\\u9fa5·]{2,16})$");


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
                    // 处理单个学生的压缩包
                    StudentHomework homework = processStudentZip(file.getAbsolutePath());
                    if (homework != null) {
                        results.add(homework);
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

        String outputJson = extractOutputAsJson(difyResponseJson);
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
                String[] parts = outputPath.split(Pattern.quote("-"));
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
                    String studentName = student.get("stu_name").asText();
                    String studentId = student.get("stu_id").asText();
                    String totalScore = student.get("total_score").asText();
                    String review =  student.get("review").asText();
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
                log.error("JSON 中未找到 'output' 键");
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
    @Override
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
     * 调用Dify工作流（项目zip模式）
     * @param zipFilePath 项目zip文件路径
     * @param rubric 评分标准
     * @return 完整拼接后的字符串（包含 Dify 吐出来的所有块）
     */
    @Override
    public String runWorkflowWithProjectZip(String zipFilePath, String rubric) {
        DataConfig dataConfig = ConfigLoader.getConfig().getData();
        String taskDir = dataConfig.getTask();
        String resultDir = dataConfig.getResult();

        GradingTask gradingTask = new GradingTask();
        gradingTask.setId(UUID.randomUUID().toString());
        gradingTask.setFileId(zipFilePath);
        gradingTask.setStatus(0);
        gradingTask.setCreateTime(LocalDateTime.now());

        GradingResult gradingResult = new GradingResult();
        gradingResult.setTaskId(gradingTask.getId());
        gradingResult.setStatus(1);
        gradingResult.setCreateTime(LocalDateTime.now());

        // 先解压zip文件，转为md文件并调用Dify工作流
        try {
            List<StudentHomework> studentHomeworks = extractFromTotalZip(zipFilePath);
            // 调用Dify工作流
            // 检查学生作业列表是否为空
            if (studentHomeworks.isEmpty()) {
                throw new BusinessException(ErrorCodeConstant.FILE_NOT_FOUND,ErrorConstant.FILES_NOT_FOUND);}
            Map<String, Object> requestBody  = new HashMap<>();
            if (null != rubric && !rubric.isEmpty()) {
                requestBody.put("rubric", rubric);
            }
            requestBody.put("upload_filesOFmd", studentHomeworks);
            requestBody.put("handle_type", "project_zip");
            log.info("调用Dify工作流");
            //工作流返回的Markdown字符串，转换为Word文档
            try {
                String result = difyClient.runWorkflowBlocking(difyProperty.getApiKey(), requestBody);
                log.info("Dify工作流成功调用，开始转换为Word文档");
                Map<String, Object> json = handleProject_zip(result);
                if (json == null) {
                    throw new RuntimeException("Dify 返回结果解析失败");
                }
                JsonNode outputNode = mapper.valueToTree(json.getOrDefault("reviews", null));
                ObjectNode resultJson = mapper.createObjectNode();
                resultJson.set("output", outputNode);
                String wordJsonStr = mapper.writeValueAsString(resultJson);
                String wordPath = mdConvertToWord(wordJsonStr).get("wordPath");
                log.info("开始生成Excel文档");
                json.remove("reviews");
                Map<String, String> excelResult = generateExcel(json);
                String excelPath = excelResult != null ? excelResult.get("excelPath") : null;

                // 保存处理结果到文件
                gradingTask.setStatus(2);
                gradingTask.setFinishTime(LocalDateTime.now());
                FileUtil.ensureDirExists(taskDir);
                FileUtil.writeJson(taskDir + File.separator + gradingTask.getId() + ".json", gradingTask);

                gradingResult.setStatus(0);
                gradingResult.setWordPath(wordPath);
                gradingResult.setExcelPath(excelPath);
                gradingResult.setExpireTime(LocalDateTime.now().plusDays(7));
                FileUtil.ensureDirExists(resultDir);
                FileUtil.writeJson(resultDir + File.separator + gradingResult.getTaskId() + ".json", gradingResult);
                return "wordPath=" + wordPath + "&excelPath=" + excelPath;


            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new BusinessException(ErrorCodeConstant.UNKNOWN_ERROR,ErrorConstant.UNKNOWN_ERROR);
            }
        } catch (BusinessException e) {
            if(e.getCode() == ErrorCodeConstant.FILE_NOT_FOUND){
                // 保存处理失败任务到文件
                gradingTask.setStatus(4);
                gradingTask.setErrorMessage(e.getMessage());
                gradingTask.setRetryCount(0);
                gradingTask.setFinishTime(LocalDateTime.now());
                try {
                    FileUtil.ensureDirExists(taskDir);
                    FileUtil.writeJson(taskDir + File.separator + gradingTask.getId() + ".json", gradingTask);
                } catch (Exception ex) {
                    log.error("保存失败任务文件异常: {}", ex.getMessage());
                }
            }
            throw e;
        }catch (Exception e) {
            log.error(e.getMessage());
            throw new BusinessException(ErrorCodeConstant.UNKNOWN_ERROR,ErrorConstant.UNKNOWN_ERROR);
        }

    }

    /**
     * 生成Excel文档
     * TODO
     * @param json
     * @return
     */
    private Map<String, String> generateExcel(Map<String, Object> json) {
        return null;
    }

}
