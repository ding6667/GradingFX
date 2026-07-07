package com.javascene.gradingfx.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GradingResult {
    private String taskId;
    private int status; // 0=成功, 1=处理中
    private LocalDateTime createTime;
    private String wordPath;
    private String excelPath;
    private LocalDateTime expireTime;
}
