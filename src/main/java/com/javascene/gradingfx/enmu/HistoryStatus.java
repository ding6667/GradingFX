package com.javascene.gradingfx.enmu;

public enum HistoryStatus {
    PENDING, // 待处理
    GRADING, // 评分中
    COMPLETED, // 已完成
    PARTIAL_FAILURE, // 部分失败
    FAILED; // 完全失败

    public String getDisplayName() {
        return switch (this) {
            case PENDING -> "待处理";
            case GRADING -> "评分中";
            case COMPLETED -> "已完成";
            case PARTIAL_FAILURE -> "部分失败";
            case FAILED -> "失败";
        };
    }
}
