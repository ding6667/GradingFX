package com.javascene.gradingfx.service;
import java.util.List;
import java.io.IOException;
public interface StandardService {
    String getCurrentStandard();
    void saveCurrentStandard(String text) throws Exception;


    // 3. 保存自定义模板（xxx.json）
    void saveCustomTemplate(String templateName, String text) throws Exception;

    // 4. 获取所有模板名称列表
    List<String> getAllTemplateNames();

    // 5. 加载指定模板内容
    String loadTemplate(String templateName) throws Exception;

    // 6. 删除指定模板文件
    boolean deleteTemplate(String templateName);

    List<String> getAllSaveTemplateName() throws IOException;

    String loadOtherTemplateByName(String templateName) throws IOException;

}

