package com.javascene.gradingfx.service;

import com.javascene.gradingfx.model.StudentResult;

import java.util.List;

public interface ExportService {

    /**
     * 导出汇总 Excel 文件（覆盖写入）
     * @param students 全部学生结果列表
     * @param outputPath 输出路径（含文件名，如 {resultPath}/{taskId}/summary.xlsx）
     */
    void exportExcel(List<StudentResult> students, String outputPath);


    /**
     * 为单个学生导出 TXT 文件（覆盖写入）
     * @param student 学生结果
     * @param outputPath 输出路径（含文件名，如 {resultPath}/{taskId}/{studentId}.txt）
     */
    void exportTxt(StudentResult student, String outputPath);
}