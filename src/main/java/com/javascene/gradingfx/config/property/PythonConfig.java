package com.javascene.gradingfx.config.property;

import lombok.Data;

@Data
public class PythonConfig {
    private String exe = "python";
    private ScriptConfig script = new ScriptConfig();

    @Data
    public static class ScriptConfig {
        private String mdToDocxPath;
        private String docxToMdPath;
    }
}