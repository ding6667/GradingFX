package com.javascene.gradingfx.service.Impl;

import com.javascene.gradingfx.model.StandardConfig;
import com.javascene.gradingfx.service.StandardService;
import com.javascene.gradingfx.util.ConfigLoader;
import com.javascene.gradingfx.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import com.javascene.gradingfx.config.property.AppConfig;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.util.List;
@Slf4j
public class StandardServiceImpl implements StandardService {
    private AppConfig getAppConfig() {
        return ConfigLoader.getConfig();
    }
    @Override
    public String getCurrentStandard() {
        try {
            String path = ConfigLoader.getConfig().getData().getCurrentStandard();
            if (path == null || !FileUtil.exists(path)) {
                return null;
            }
            StandardConfig config = FileUtil.readJson(path, StandardConfig.class);
            return config != null ? config.getText() : null;
        } catch (IOException e) {
            log.error("读取评分标准失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void saveCurrentStandard(String text) throws Exception {
        AppConfig appConfig = getAppConfig();
        String path = appConfig.getData().getCurrentStandard();
        StandardConfig saveConfig = new StandardConfig();
        saveConfig.setText(text);
        FileUtil.writeJson(path, saveConfig);
    }

    @Override
    public void saveCustomTemplate(String templateName, String text) throws Exception {
        AppConfig appConfig = getAppConfig();
        String standardDirPath = appConfig.getData().getStandard();
        File dir = new File(standardDirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String savePath = standardDirPath + File.separator + templateName + ".json";
        StandardConfig temp = new StandardConfig();
        temp.setText(text);
        FileUtil.writeJson(savePath, temp);
    }

    @Override
    public List<String> getAllTemplateNames() {
        List<String> nameList = new ArrayList<>();
        AppConfig appConfig = getAppConfig();
        String standardDirPath = appConfig.getData().getStandard();
        File dir = new File(standardDirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return nameList;
        }
        File[] allFiles = dir.listFiles();
        if (allFiles == null) return nameList;
        for (File file : allFiles) {
            String fileName = file.getName();
            if (fileName.endsWith(".json")) {
                String templateName = fileName.replace(".json", "");
                nameList.add(templateName);
            }
        }
        return nameList;
    }

    @Override
    public String loadTemplate(String templateName) throws Exception {
        AppConfig appConfig = getAppConfig();
        String standardDirPath = appConfig.getData().getStandard();
        String templateFilePath = standardDirPath + File.separator + templateName + ".json";
        File templateFile = new File(templateFilePath);
        if (!templateFile.exists()) {
            return null;
        }
        StandardConfig tempConfig = FileUtil.readJson(templateFilePath, StandardConfig.class);
        return tempConfig.getText() == null ? "" : tempConfig.getText();
    }

    @Override
    public boolean deleteTemplate(String templateName) {
        AppConfig appConfig = getAppConfig();
        String standardDirPath = appConfig.getData().getStandard();
        String templateFilePath = standardDirPath + File.separator + templateName + ".json";
        File templateFile = new File(templateFilePath);
        if (templateFile.exists()) {
            return templateFile.delete();
        }
        return false;
    }
    @Override
    public List<String> getAllSaveTemplateName() throws IOException {
        // 1.读取yml里standard文件夹路径
        AppConfig config = ConfigLoader.getConfig();
        String standardDirPath = config.getData().getStandard();
        File dir = new File(standardDirPath);
        List<String> templateNames = new ArrayList<>();

        // 文件夹不存在直接返回空集合
        if (!dir.exists() || !dir.isDirectory()) {
            return templateNames;
        }
        // 筛选json模板文件
        File[] files = dir.listFiles((file) -> file.getName().endsWith(".json"));
        if (files == null) return templateNames;

        for (File file : files) {
            // 去掉后缀.json，只保留模板名称
            String name = file.getName().replace(".json", "");
            templateNames.add(name);
        }
        return templateNames;
    }

    @Override
    public String loadOtherTemplateByName(String templateName) throws IOException {
        AppConfig config = ConfigLoader.getConfig();
        // 1.读取选中模板文件
        String standardDir = config.getData().getStandard();
        String templatePath = standardDir + File.separator + templateName + ".json";
        StandardConfig template = FileUtil.readJson(templatePath, StandardConfig.class);

        // 2.同步写入currentstandard（更新当前使用模板）
        String currentPath = config.getData().getCurrentStandard();
        FileUtil.writeJson(currentPath, template);

        // 返回文本内容给界面
        return template.getText();
    }
    @Override
    public boolean setCurrentDefaultStandard(String text) {
        // 1. 读取yml配置里currentstandard文件路径
        AppConfig appConfig = ConfigLoader.getConfig();
        String path = appConfig.getData().getCurrentStandard();
        File file = new File(path);
        // 2. 父文件夹不存在就自动创建，防止报错
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        // 3. 封装实体类写入json
        StandardConfig config = new StandardConfig();
        config.setText(text);
        try {
            FileUtil.writeJson(path, config);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


}


