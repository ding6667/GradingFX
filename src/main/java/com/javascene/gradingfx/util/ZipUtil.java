package com.javascene.gradingfx.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtil {

    /**
     * 递归查找目录下的 Word 文档（.docx 或 .doc）
     */
    public static File findWordFile(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isDirectory()) {
                File found = findWordFile(f);
                if (found != null) return found;
            } else {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".docx") || name.endsWith(".doc")) {
                    return f;
                }
            }
        }
        return null;
    }

    /**
     * 解压 Zip 文件到目标目录
     */
    public static void unzip(String zipFilePath, File destDir) throws IOException {
        destDir.mkdirs();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath), Charset.forName("GBK"))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }
}
