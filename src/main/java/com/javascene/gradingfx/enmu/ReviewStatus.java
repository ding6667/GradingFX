package com.javascene.gradingfx.enmu;

public enum ReviewStatus {
    PENDING, // 待批阅
    APPROVED, // 已批阅
    PROCESSING, // 处理中
    FAILED; // 处理失败

    public String getDisplayName() {
        return switch (this) {
            case PENDING -> "待批阅";
            case APPROVED -> "已批阅";
            case PROCESSING -> "处理中";
            case FAILED -> "处理失败";
        };
    }
}
