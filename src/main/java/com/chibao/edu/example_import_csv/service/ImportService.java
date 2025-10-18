package com.chibao.edu.example_import_csv.service;

import com.chibao.edu.example_import_csv.common.ImportOption;
import com.chibao.edu.example_import_csv.dto.ImportJobStatusDto;
import com.chibao.edu.example_import_csv.dto.ImportPreviewResponse;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public interface ImportService {
    ImportPreviewResponse preview(InputStream is) throws IOException;
    UUID startImport(InputStream is, ImportOption option) throws IOException;
    ImportJobStatusDto getStatus(UUID jobId);
    Resource getErrorReport(UUID jobId);
}
