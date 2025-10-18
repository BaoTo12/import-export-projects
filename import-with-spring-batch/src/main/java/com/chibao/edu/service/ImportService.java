package com.chibao.edu.service;

import com.chibao.edu.common.ImportOption;
import com.chibao.edu.dto.ImportPreviewResponse;

import java.io.IOException;
import java.util.UUID;

public interface ImportService {
    ImportPreviewResponse previewFromFile(String filePath) throws IOException;
    void startImportFromStoredFile(UUID jobId, ImportOption option);
}
