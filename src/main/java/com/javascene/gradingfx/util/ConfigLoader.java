package com.javascene.gradingfx.util;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.javascene.gradingfx.config.property.AppConfig;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
            .findAndRegisterModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private static volatile AppConfig instance;

    static {
        load();
    }

    private static void load() {
        try {
            try (InputStream in = ConfigLoader.class.getClassLoader()
                    .getResourceAsStream("application.yml")) {
                if (in != null) {
                    instance = MAPPER.readValue(in, AppConfig.class);
                    System.out.println("加载类路径配置: application.yml");
                } else {
                    System.err.println("未找到 application.yml，使用默认配置");
                    instance = new AppConfig();
                }
            }
        } catch (Exception e) {
            System.err.println("加载配置文件失败: " + e.getMessage());
            instance = new AppConfig();
        }
    }

    public static AppConfig getConfig() {
        return instance;
    }

    public static void reload() {
        load();
    }


}