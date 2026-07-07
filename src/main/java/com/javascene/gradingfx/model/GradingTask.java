package com.javascene.gradingfx.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GradingTask {
    private String id;
    private String fileId;
    private int status; // 0=处理中, 2=成功, 4=失败
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
    private String errorMessage;
    private int retryCount;
}
