package com.javascene.gradingfx.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentHomework {
    private String studentId;
    private String studentName;
    private String wordContent;
}
