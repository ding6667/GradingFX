package com.javascene.gradingfx.config.property;

import lombok.Data;

@Data
public class AppConfig {
    private AppInfo app = new AppInfo();
    private DifyConfig dify = new DifyConfig();
    private PythonConfig python = new PythonConfig();
    private FileConfig file = new FileConfig();
    private DataConfig data = new DataConfig();

    public AppConfig() {
        // 设置默认配置值
        dify.getApi().setBaseUrl("http://localhost:5001/v1");
        dify.getApi().setApiKey("app-h2yIzEsq6W2C5eHN0MtDVRt2");
    }
}
