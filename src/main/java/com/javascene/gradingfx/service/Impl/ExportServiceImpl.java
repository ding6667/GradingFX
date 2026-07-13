package com.javascene.gradingfx.service.Impl;

import com.javascene.gradingfx.config.property.AppConfig;
import com.javascene.gradingfx.model.StudentResult;
import com.javascene.gradingfx.service.ExportService;
import com.javascene.gradingfx.util.ConfigLoader;
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

    private static final String[] EXCEL_HEADERS = {"序号", "学号", "姓名", "评分"};
    private final AppConfig  appConfig = ConfigLoader.getConfig();

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
                setCellValue(row, 0, String.valueOf(i + 1));
                setCellValue(row, 1, s.getStudentId());
                setCellValue(row, 2, s.getStudentName());
                String finalScore = (s.getTeacherScore() != null && !s.getTeacherScore().isEmpty())
                        ? s.getTeacherScore() : s.getRawScore();
                setCellValue(row, 3, finalScore);
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


    private void setCellValue(XSSFRow row, int col, String value) {
        XSSFCell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }
}
