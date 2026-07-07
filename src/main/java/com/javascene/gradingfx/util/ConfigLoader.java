package com.javascene.gradingfx.util;


import com.javascene.gradingfx.config.property.AppConfig;
import org.yaml.snakeyaml.Yaml;


import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 配置加载器，负责从 YAML 文件加载配置
 */
public class ConfigLoader {
    private static volatile AppConfig instance;

    static {
        load();
    }

    private static void load() {
        try {
            // 优先尝试加载外部配置文件（方便打包后修改）
            Path externalConfig = Paths.get("./config/application.yml");
            if (Files.exists(externalConfig)) {
                try (InputStream in = Files.newInputStream(externalConfig)) {
                    instance = parseYaml(in);
                    System.out.println("加载外部配置: " + externalConfig.toAbsolutePath());
                    return;
                }
            }

            // 从类路径加载（开发时放在 src/main/resources）
            try (InputStream in = ConfigLoader.class.getClassLoader()
                    .getResourceAsStream("application.yml")) {
                if (in != null) {
                    instance = parseYaml(in);
                    System.out.println("加载类路径配置: application.yml");
                } else {
                    System.err.println("未找到 application.yml，使用默认配置");
                    instance = new AppConfig();
                }
            }
        } catch (Exception e) {
            System.err.println("加载配置文件失败: " + e.getMessage());
            instance = new AppConfig(); // fallback 默认空配置
        }
    }

    private static AppConfig parseYaml(InputStream in) {
        Yaml yaml = new Yaml();
        return yaml.loadAs(in,AppConfig.class);
    }

    /**
     * 获取配置实例（单例）
     */
    public static AppConfig getConfig() {
        return instance;
    }

    /**
     * 热重载配置（用于修改配置后无需重启）
     */
    public static void reload() {
        load();
    }
}