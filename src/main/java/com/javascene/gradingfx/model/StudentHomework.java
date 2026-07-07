package com.javascene.gradingfx.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学生作业实体 —— 从zip中解析的学生作业信息（临时中转对象，不持久化）
 * 用于传递给 Dify 工作流 API 作为请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentHomework {
    /** 学生学号（从zip文件名正则提取，10位数字） */
    private String studentId;
    /** 学生姓名（从zip文件名正则提取，中文名） */
    private String studentName;
    /** Word文档转换为Markdown后的内容 */
    private String wordContent;
}
