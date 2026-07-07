package com.javascene.gradingfx.service;

import java.util.List;
import java.util.Map;

public interface ReviewService {
    Map<String, List<String>> wordConvertToMd(List<String> fileUrls);
    Map<String, String> mdConvertToWord(String mdContent);

    String runWorkflowWithCommonFiles(List<String> fileUrls, String rubric);
    String runWorkflowWithProjectZip(String id, String rubric);
}
