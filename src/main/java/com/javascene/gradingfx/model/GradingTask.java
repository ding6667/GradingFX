package com.javascene.gradingfx.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 批阅任务实体 —— 记录一次批量批阅的任务信息
 * 持久化到 data/task/tasks.json（List 模式）
 */
@Data
public class GradingTask {
    /** 状态常量：处理中 */
    public static final int STATUS_PROCESSING = 0;
    /** 状态常量：成功 */
    public static final int STATUS_SUCCESS = 2;
    /** 状态常量：失败 */
    public static final int STATUS_FAILED = 4;

    /** 任务唯一标识（UUID） */
    private String id;
    /** 上传的zip文件路径 */
    private String filePath;
    /** 任务名称（取自zip文件名） */
    private String taskName;
    /** 学生总数 */
    private int totalStudents;
    /** 已批阅学生数 */
    private int gradedStudents;
    /** 任务状态：0=处理中, 2=成功, 4=失败 */
    private int status;
    /** 任务创建时间 */
    private LocalDateTime createTime;
    /** 任务完成时间（成功或失败时设置） */
    private LocalDateTime finishTime;
    /** 错误信息（失败时记录） */
    private String errorMessage;
    /** 重试次数 */
    private int retryCount;
}
