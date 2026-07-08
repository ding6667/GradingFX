package com.javascene.gradingfx.service.Impl;

import com.javascene.gradingfx.model.StandardConfig;
import com.javascene.gradingfx.service.StandardService;
import com.javascene.gradingfx.util.ConfigLoader;
import com.javascene.gradingfx.util.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class StandardServiceImpl implements StandardService {

    @Override
    public String getCurrentStandard() {
        try {
            String path = ConfigLoader.getConfig().getData().getCurrentStandard();
            if (path == null || !FileUtil.exists(path)) {
                return null;
            }
            StandardConfig config = FileUtil.readJson(path, StandardConfig.class);
            return config != null ? config.getCurrentStandard() : null;
        } catch (IOException e) {
            log.error("读取评分标准失败: {}", e.getMessage());
            return null;
        }
    }
}