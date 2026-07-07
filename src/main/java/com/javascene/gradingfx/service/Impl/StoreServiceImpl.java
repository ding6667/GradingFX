package com.javascene.gradingfx.service.Impl;

import com.javascene.gradingfx.service.StoreService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class StoreServiceImpl implements StoreService {
    /**
     * 存储文件
     * @param file 文件对象
     * @param fileId 文件ID
     * @return 文件路径字符串
     * @throws IOException
     */
    @Override
    public String store(MultipartFile file, String fileId) throws IOException {
        // 按日期生成文件夹
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path basePath = Paths.get(tempZip).resolve(dir);

        Files.createDirectories(basePath);
        // 生成唯一文件名
        String fileName = fileId + ".zip";
        Path targetPath = basePath.resolve(fileName);
        file.transferTo(targetPath);

        return targetPath.toString();

    }

    @Override
    public Integer delete(String filePath) {
        try {
            Files.delete(Path.of(filePath));
            return 1;
        } catch (IOException e) {
            log.error("删除文件失败", e);
            return 0;

        }

    }
}
