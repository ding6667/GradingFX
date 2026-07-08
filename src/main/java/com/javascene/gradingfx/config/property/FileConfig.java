package com.javascene.gradingfx.config.property;

import lombok.Data;

@Data
public class FileConfig {
    private String zipPath;
    private String unzipPath;
    private String tempPath;
    private String wordPath;
    private String excelPath;
    /** 结果导出路径（存放 TXT/Excel/Word 等导出文件，含子文件夹 taskId） */
    private String resultPath;
}
