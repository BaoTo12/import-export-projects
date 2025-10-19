package com.chibao.edu.service.impl;


import com.chibao.edu.async.processor.ImportAsyncProcessorCSV;
import com.chibao.edu.async.processor.ImportAsyncProcessorExcel;
import com.chibao.edu.common.ImportJob;
import com.chibao.edu.common.ImportOption;
import com.chibao.edu.common.ParseResult;
import com.chibao.edu.common.RowResult;
import com.chibao.edu.dto.ImportPreviewResponse;
import com.chibao.edu.parser.impl.ExcelPatientParser;
import com.chibao.edu.repository.ImportJobRepository;
import com.chibao.edu.service.ImportService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ImportServiceImpl implements ImportService {
    ExcelPatientParser parser;
    ImportJobRepository importJobRepository;
    ImportAsyncProcessorExcel asyncProcessor;


    @Override
    public ImportPreviewResponse previewFromFile(String filePath) throws IOException {
        ParseResult pr = parser.parse(filePath);
        UUID jobId = UUID.randomUUID();

        List<RowResult> previewRows = pr.getRows().stream().limit(20).collect(Collectors.toList());
        ImportPreviewResponse resp = ImportPreviewResponse.of(jobId, pr.isHeaderValid(), previewRows, pr.getMessage());
        resp.setTotalRows(pr.getRows().size());
        resp.setTooManyRows(pr.isTooManyRows());
        return resp;
    }

    @Override
    public void startImportFromStoredFile(UUID jobId, ImportOption option) {
        ImportJob j = importJobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        String path = j.getFilePath();

        try {
            ParseResult pr = parser.parse(path);
            if (!pr.isHeaderValid()) throw new IllegalArgumentException("Invalid headers: " + pr.getMessage());
            if (pr.isTooManyRows()) throw new IllegalArgumentException("Too many rows");

            j.setStatus("RUNNING");
            importJobRepository.save(j);

            asyncProcessor.processAsync(jobId, pr.getRows(), option);
        } catch (IOException ex) {
            j.setStatus("FAILED");
            j.setErrorFile(null);
            j.setCompletedAt(LocalDateTime.now());
            importJobRepository.save(j);
            throw new RuntimeException(ex);
        }
    }
}
