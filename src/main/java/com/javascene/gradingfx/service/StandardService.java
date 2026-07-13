package com.javascene.gradingfx.service;
import java.util.List;
import java.io.IOException;
public interface StandardService {
    String getCurrentStandard();
    void saveCurrentStandard(String text) throws Exception;

    void saveCustomTemplate(String templateName, String text) throws Exception;

    List<String> getAllTemplateNames();

    String loadTemplate(String templateName) throws Exception;

    boolean deleteTemplate(String templateName);

    List<String> getAllSaveTemplateName() throws IOException;

    String loadOtherTemplateByName(String templateName) throws IOException;

    boolean setCurrentDefaultStandard(String text);

}

