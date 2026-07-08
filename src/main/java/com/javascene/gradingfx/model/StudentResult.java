package com.javascene.gradingfx.model;

import lombok.Data;

/**
 * 学生成绩实体 —— 记录每个学生的批阅成绩详情
 * 持久化到 data/result/scores.json（List 模式）
 * 通过 taskId 关联 GradingTask，转换为 StudentResult 供 UI 显示
 */
@Data
public class StudentResult {
    /** 状态常量 */
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_FAILED = "FAILED";

    /** 关联的任务ID */
    private String taskId;
    /** 学生学号 */
    private String studentId;
    /** 学生姓名 */
    private String studentName;
    /** AI原始评分 */
    private String rawScore;
    /** AI批阅评语（Markdown格式） */
    private String aiComment;
    /** 教师修改后的分数 */
    private String teacherScore;
    /** 教师批阅评语 */
    private String teacherComment;
    /** 教师备注 */
    private String teacherNote;
    /** 状态：PENDING=待批阅, PROCESSING=处理中, APPROVED=已批阅, FAILED=失败 */
    private String status;
    /** 错误信息（处理失败时记录） */
    private String errorMessage;
    /** 学生作业内容（Markdown格式，用于调用Dify，持久化以便重试） */
    private String wordContent;
}
