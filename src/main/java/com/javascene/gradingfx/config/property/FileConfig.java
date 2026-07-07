package com.javascene.gradingfx.config.property;

import lombok.Data;

@Data
public class FileConfig {
    private String zipPath;
    private String unzipPath;
    private String tempPath;
    private String wordPath;
    private String excelPath;
}