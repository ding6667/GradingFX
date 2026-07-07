package com.javascene.gradingfx.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentHomework {
    private String studentId;
    private String studentName;
    private String wordContent;

    @Override
    public String toString() {
        return String.format("学号: %s, 姓名: %s, 文档长度: %d字符", studentId, studentName, wordContent.length());
    }
}