package com.javascene.gradingfx.service;

import java.io.File;
import java.io.IOException;

public interface StoreService {
    String store(File file, String fileId) throws IOException;
    Integer delete(String filePath);
}
