package com.javascene.gradingfx.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导出路径配置实体 —— 持久化到 data/exportConfig.json
 * 记录用户配置的导出路径和文件名模板
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportConfig {
    /** 导出目录路径 */
    private String outputPath;
    /** 文件名模板，支持变量: {date} 日期, {taskName} 任务名称 */
    private String fileNameTemplate;
}
