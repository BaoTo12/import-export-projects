package com.chibao.edu.service.impl;


import com.chibao.edu.common.ImportJob;
import com.chibao.edu.common.ImportOption;
import com.chibao.edu.common.ParseResult;
import com.chibao.edu.common.RowResult;
import com.chibao.edu.dto.ImportPreviewResponse;
import com.chibao.edu.entity.Patient;
import com.chibao.edu.parser.ExcelPatientParser;
import com.chibao.edu.repository.ImportJobRepository;
import com.chibao.edu.repository.PatientRepository;
import com.chibao.edu.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImportServiceImpl implements ImportService {
    private final ExcelPatientParser parser; // since we focus on Excel
    private final ImportJobRepository importJobRepository;
    private final PatientRepository patientRepository;
    private final Path errorReportDir = Paths.get(System.getProperty("java.io.tmpdir"), "excel-import-errors");

    @Override
    public ImportPreviewResponse previewFromFile(String filePath) throws IOException {
        ParseResult pr = parser.parse(filePath);
        UUID jobId = UUID.randomUUID(); // preview-only id - actual persistent job created by controller


        List<RowResult> previewRows = pr.getRows().stream().limit(20).collect(Collectors.toList());
        ImportPreviewResponse resp = ImportPreviewResponse.of(jobId, pr.isHeaderValid(), previewRows, pr.getMessage());
        resp.setTotalRows(pr.getRows().size());
        resp.setTooManyRows(pr.isTooManyRows());
        return resp;
    }

    @Override
    public void startImportFromStoredFile(UUID jobId, ImportOption option) {
        // find job record to get path
        ImportJob j = importJobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        String path = j.getFilePath();
        try {
            ParseResult pr = parser.parse(path);
            if (!pr.isHeaderValid()) throw new IllegalArgumentException("Invalid headers: " + pr.getMessage());
            if (pr.isTooManyRows()) throw new IllegalArgumentException("Too many rows");

            // persist job status
            j.setStatus("RUNNING");
            importJobRepository.save(j);
            // start async processing
            processAsync(jobId, pr.getRows(), option);
        } catch (IOException ex) {
            j.setStatus("FAILED");
            j.setErrorFile(null);
            j.setCompletedAt(LocalDateTime.now());
            importJobRepository.save(j);
            throw new RuntimeException(ex);
        }
    }

    @Async("importTaskExecutor")
    @Transactional
    public void processAsync(UUID jobId, List<RowResult> rows, ImportOption option) {
        ImportJob job = importJobRepository.findById(jobId).orElseThrow();
        job.setStatus("RUNNING");
        importJobRepository.save(job);

        List<String[]> errorCsvRows = new ArrayList<>();
        int success = 0;
        int failed = 0;
        int batchSize = 100;
        List<Patient> toSave = new ArrayList<>();

        try {
            for (int i = 0; i < rows.size(); i++) {
                RowResult rr = rows.get(i);
                if (!rr.getErrors().isEmpty()) {
                    failed++;
                    errorCsvRows.add(toErrorRow(i + 1, rr.getErrors(), rr));
                    if (option == ImportOption.FAIL) {
                        job.setStatus("FAILED");
                        job.setCompletedAt(LocalDateTime.now());
                        importJobRepository.save(job);
                        writeErrors(jobId, errorCsvRows);
                        return;
                    }
                    continue;
                }
                Optional<Patient> existing = patientRepository.findByNationalId(rr.getNationalId());
                if (existing.isPresent()) {
                    switch (option) {
                        case SKIP:
                            success++; // skipped considered success
                            break;
                        case UPDATE:
                            Patient p = existing.get();
                            applyRowToPatient(rr, p);
                            toSave.add(p);
                            break;
                        case FAIL:
                            failed++;
                            errorCsvRows.add(toErrorRow(i + 1, List.of("Duplicate nationalId"), rr));
                            job.setStatus("FAILED");
                            job.setCompletedAt(LocalDateTime.now());
                            importJobRepository.save(job);
                            writeErrors(jobId, errorCsvRows);
                            return;
                    }
                } else {
                    Patient pNew = new Patient();
                    applyRowToPatient(rr, pNew);
                    toSave.add(pNew);
                }
                if (toSave.size() >= batchSize) {
                    patientRepository.saveAll(toSave);
                    // flush not available, but saveAll should persist
                    success += toSave.size();
                    toSave.clear();
                    job.setStatus("RUNNING");
                    importJobRepository.save(job);
                }
            }
            if (!toSave.isEmpty()) {
                patientRepository.saveAll(toSave);
                success += toSave.size();
            }
            job.setStatus(errorCsvRows.isEmpty() ? "SUCCESS" : "PARTIAL_FAILED");
            job.setCompletedAt(LocalDateTime.now());
            if (!errorCsvRows.isEmpty()) {
                String errorsPath = writeErrors(jobId, errorCsvRows);
                job.setErrorFile(errorsPath);
            }
            importJobRepository.save(job);
        } catch (Exception ex) {
            job.setStatus("FAILED");
            job.setCompletedAt(LocalDateTime.now());
            importJobRepository.save(job);
        }

    }

    private String writeErrors(UUID jobId, List<String[]> errorRows) throws IOException {
        Files.createDirectories(errorReportDir);
        Path out = errorReportDir.resolve("errors-" + jobId + ".csv");
        try (BufferedWriter writer = Files.newBufferedWriter(out);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("row", "errors", "firstName","lastName","email","phone","nationalId","dob"))) {
            for (String[] row : errorRows) printer.printRecord((Object[]) row);
        }
        return out.toAbsolutePath().toString();
    }

    private String[] toErrorRow(int rowNumber, List<String> errs, RowResult rr) {
        return new String[]{
                String.valueOf(rowNumber),
                String.join("; ", errs),
                rr.getFirstName(), rr.getLastName(), rr.getEmail(), rr.getPhone(), rr.getNationalId(),
                rr.getDob() == null ? "" : rr.getDob().toString()
        };
    }

    private void applyRowToPatient(RowResult r, Patient p) {
        p.setFirstName(r.getFirstName());
        p.setLastName(r.getLastName());
        p.setEmail(r.getEmail());
        p.setPhone(r.getPhone());
        p.setNationalId(r.getNationalId());
        p.setDob(r.getDob());
    }
}
