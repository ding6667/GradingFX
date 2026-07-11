package com.javascene.gradingfx.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 批阅结果实体 —— 记录一次批阅产出的文件路径信息
 * 持久化到 data/result/results.json（List 模式）
 * 通过 taskId 关联 GradingTask
 */
@Data
public class GradingResult {
    /** 关联的任务ID */
    private String taskId;
    /** 结果状态：0=成功, 1=处理中 */
    private int status;
    /** 结果创建时间 */
    private LocalDateTime createTime;
    /** Word批阅报告文件路径 */
    private String wordPath;
    /** Excel成绩表文件路径（可能为null，Excel功能未实现时） */
    private String excelPath;
    /** 评分变更标记：0=未变更, 1=已变更（导出Excel时需重新生成） */
    private int scoreChanged = 0;
    /** 结果过期时间（默认创建后7天） */
    private LocalDateTime expireTime;
}
