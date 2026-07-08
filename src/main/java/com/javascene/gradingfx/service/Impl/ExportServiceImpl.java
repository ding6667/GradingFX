package com.javascene.gradingfx.service.Impl;

import com.javascene.gradingfx.model.StudentResult;
import com.javascene.gradingfx.service.ExportService;
import com.javascene.gradingfx.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 导出服务实现：使用 Apache POI 生成 Excel/Word，原生 IO 生成 TXT
 */
@Slf4j
public class ExportServiceImpl implements ExportService {

    private static final String[] EXCEL_HEADERS = {"学号", "姓名", "AI评分", "教师评分", "状态", "错误信息"};

    @Override
    public void exportExcel(List<StudentResult> students, String outputPath) {
        log.info("开始生成 Excel: {}", outputPath);
        try { FileUtil.ensureDirExists(new File(outputPath).getParentFile().getAbsolutePath()); } catch (IOException e) { log.error("创建目录失败: {}", e.getMessage()); }
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("批阅结果");

            // 表头
            XSSFRow headerRow = sheet.createRow(0);
            for (int i = 0; i < EXCEL_HEADERS.length; i++) {
                XSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(EXCEL_HEADERS[i]);
            }

            // 数据行
            for (int i = 0; i < students.size(); i++) {
                StudentResult s = students.get(i);
                XSSFRow row = sheet.createRow(i + 1);
                setCellValue(row, 0, s.getStudentId());
                setCellValue(row, 1, s.getStudentName());
                setCellValue(row, 2, s.getRawScore());
                setCellValue(row, 3, s.getTeacherScore());
                setCellValue(row, 4, s.getStatus());
                setCellValue(row, 5, s.getErrorMessage());
            }

            // 自适应列宽
            for (int i = 0; i < EXCEL_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                workbook.write(fos);
            }
            log.info("Excel 生成成功: {}", outputPath);
        } catch (IOException e) {
            log.error("生成 Excel 失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void exportWord(List<StudentResult> students, String outputPath) {
        log.info("开始生成 Word: {}", outputPath);
        try { FileUtil.ensureDirExists(new File(outputPath).getParentFile().getAbsolutePath()); } catch (IOException e) { log.error("创建目录失败: {}", e.getMessage()); }
        try (XWPFDocument doc = new XWPFDocument()) {
            // 标题
            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText("作业批阅结果汇总");
            titleRun.setBold(true);
            titleRun.setFontSize(18);

            // 空行
            doc.createParagraph();

            // 逐个学生
            for (StudentResult s : students) {
                // 学号姓名
                XWPFParagraph header = doc.createParagraph();
                XWPFRun headerRun = header.createRun();
                headerRun.setText("学号: " + nvl(s.getStudentId()) + "    姓名: " + nvl(s.getStudentName()));
                headerRun.setBold(true);
                headerRun.setFontSize(12);

                // 评分
                XWPFParagraph score = doc.createParagraph();
                XWPFRun scoreRun = score.createRun();
                scoreRun.setText("AI评分: " + nvl(s.getRawScore()) + "    教师评分: " + nvl(s.getTeacherScore()));

                // 评语
                if (s.getAiComment() != null && !s.getAiComment().isEmpty()) {
                    XWPFParagraph comment = doc.createParagraph();
                    XWPFRun commentRun = comment.createRun();
                    commentRun.setText("AI评语: " + s.getAiComment());
                }

                // 教师评语
                if (s.getTeacherComment() != null && !s.getTeacherComment().isEmpty()) {
                    XWPFParagraph tc = doc.createParagraph();
                    XWPFRun tcRun = tc.createRun();
                    tcRun.setText("教师评语: " + s.getTeacherComment());
                }

                // 错误信息
                if (s.getErrorMessage() != null && !s.getErrorMessage().isEmpty()) {
                    XWPFParagraph err = doc.createParagraph();
                    XWPFRun errRun = err.createRun();
                    errRun.setText("错误信息: " + s.getErrorMessage());
                    errRun.setColor("FF0000");
                }

                // 分隔线
                XWPFParagraph sep = doc.createParagraph();
                XWPFRun sepRun = sep.createRun();
                sepRun.setText("──────────────────────────────");
                sepRun.setFontSize(8);
            }

            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                doc.write(fos);
            }
            log.info("Word 生成成功: {}", outputPath);
        } catch (IOException e) {
            log.error("生成 Word 失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void exportTxt(StudentResult student, String outputPath) {
        try { FileUtil.ensureDirExists(new File(outputPath).getParentFile().getAbsolutePath()); } catch (IOException e) { log.error("创建目录失败: {}", e.getMessage()); }
        StringBuilder sb = new StringBuilder();
        sb.append("════════════════════════════════\n");
        sb.append("          作业批阅结果\n");
        sb.append("════════════════════════════════\n\n");
        sb.append("学　号：").append(nvl(student.getStudentId())).append("\n");
        sb.append("姓　名：").append(nvl(student.getStudentName())).append("\n");
        sb.append("状　态：").append(nvl(student.getStatus())).append("\n\n");
        sb.append("── AI 评分 ──\n");
        sb.append(nvl(student.getRawScore())).append("\n\n");

        if (student.getAiComment() != null && !student.getAiComment().isEmpty()) {
            sb.append("── AI 评语 ──\n");
            sb.append(student.getAiComment()).append("\n\n");
        }
        if (student.getTeacherScore() != null && !student.getTeacherScore().isEmpty()) {
            sb.append("── 教师评分 ──\n");
            sb.append(student.getTeacherScore()).append("\n\n");
        }
        if (student.getTeacherComment() != null && !student.getTeacherComment().isEmpty()) {
            sb.append("── 教师评语 ──\n");
            sb.append(student.getTeacherComment()).append("\n\n");
        }
        if (student.getErrorMessage() != null && !student.getErrorMessage().isEmpty()) {
            sb.append("── 错误信息 ──\n");
            sb.append(student.getErrorMessage()).append("\n");
        }

        try {
            FileUtil.write(outputPath, sb.toString());
        } catch (IOException e) {
            log.error("生成 TXT 失败 [{}]: {}", outputPath, e.getMessage());
        }
    }

    // ==================== 私有工具方法 ====================

    private void setCellValue(XSSFRow row, int col, String value) {
        XSSFCell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }
}
