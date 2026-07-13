package com.javascene.gradingfx.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);


    /**
     * 按行读取文件内容
     */
    public static List<String> readLines(String filePath) throws IOException {
        return Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
    }

    /**
     * 读取文件全部内容为字符串
     */
    public static String readAll(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
    }

    /**
     * 写入文本内容到文件（覆盖模式）
     */
    public static void write(String filePath, String content) throws IOException {
        Files.writeString(Paths.get(filePath), content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * 追加文本内容到文件
     */
    public static void append(String filePath, String content) throws IOException {
        Files.writeString(Paths.get(filePath), content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * 写入行列表到文件
     */
    public static void writeLines(String filePath, List<String> lines) throws IOException {
        Files.write(Paths.get(filePath), lines, StandardCharsets.UTF_8);
    }


    /**
     * 将对象序列化为 JSON 并写入文件
     */
    public static <T> void writeJson(String filePath, T object) throws IOException {
        MAPPER.writeValue(new File(filePath), object);
    }

    /**
     * 将 List 序列化为 JSON 并写入文件
     */
    public static <T> void writeJsonList(String filePath, List<T> list) throws IOException {
        MAPPER.writeValue(new File(filePath), list);
    }
    /**
     * 从 JSON 文件读取并反序列化为指定类型
     */
    public static <T> T readJson(String filePath, Class<T> clazz) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile() || file.length() == 0) {
            return null;
        }
        return MAPPER.readValue(file, clazz);
    }

    /**
     * 从 JSON 文件读取并反序列化为 List
     */
    public static <T> List<T> readJsonList(String filePath, Class<T> elementClass) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }
        return MAPPER.readValue(file,
                MAPPER.getTypeFactory().constructCollectionType(List.class, elementClass));
    }

    /**
     * 从 JSON 文件读取并反序列化为指定 TypeReference（适用于泛型复杂类型）
     */
    public static <T> T readJson(String filePath, TypeReference<T> typeRef) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || file.length() == 0) {
            return null;
        }
        return MAPPER.readValue(file, typeRef);
    }

    /**
     * 使用 Java 原生序列化写入对象（需实现 Serializable）
     */
    public static void writeObject(String filePath, Serializable object) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(filePath))) {
            oos.writeObject(object);
        }
    }

    /**
     * 使用 Java 原生序列化读取对象
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T readObject(String filePath)
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(filePath))) {
            return (T) ois.readObject();
        }
    }

    /**
     * 确保目录存在
     */
    public static void ensureDirExists(String dirPath) throws IOException {
        Files.createDirectories(Paths.get(dirPath).toFile().isFile() ? Paths.get(dirPath).getParent() : Paths.get(dirPath));
    }

    /**
     * 检查文件是否存在
     */
    public static boolean exists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    /**
     * 删除文件
     */
    public static boolean delete(String filePath) throws IOException {
        return Files.deleteIfExists(Paths.get(filePath));
    }

    /**
     * 复制文件
     */
    public static void copy(String from, String to) throws IOException {
        Files.copy(Paths.get(from), Paths.get(to), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 移动/重命名文件
     */
    public static void move(String from, String to) throws IOException {
        Files.move(Paths.get(from), Paths.get(to), StandardCopyOption.REPLACE_EXISTING);
    }
}