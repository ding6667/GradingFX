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
     * 导出汇总 Word 文件（覆盖写入）
     * @param students 全部学生结果列表
     * @param outputPath 输出路径（含文件名，如 {resultPath}/{taskId}/summary.docx）
     */
    void exportWord(List<StudentResult> students, String outputPath);

    /**
     * 为单个学生导出 TXT 文件（覆盖写入）
     * @param student 学生结果
     * @param outputPath 输出路径（含文件名，如 {resultPath}/{taskId}/{studentId}.txt）
     */
    void exportTxt(StudentResult student, String outputPath);

    /**
     * 为单个任务导出 Word 文件,main界面交互
     * @param taskId 任务ID
     * @return 导出的 Word 文件路径（含文件名，如 {resultPath}/{taskId}/summary.docx），失败则返回null
     */
    String exportWord(String taskId);

    /**
     * 为单个任务导出 Excel 文件,main界面交互
     * @param taskId 任务ID
     * @return 导出的 Excel 文件路径（含文件名，如 {resultPath}/{taskId}/summary.xlsx），失败则返回null
     */
    String exportExcel(String taskId);


}
