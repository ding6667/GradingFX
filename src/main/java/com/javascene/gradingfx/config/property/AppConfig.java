package com.javascene.gradingfx.config.property;

import lombok.Data;

@Data
public class AppConfig {
    private AppInfo app = new AppInfo();
    private DifyConfig dify = new DifyConfig();
    private PythonConfig python = new PythonConfig();
    private FileConfig file = new FileConfig();
    private DataConfig data = new DataConfig();

}