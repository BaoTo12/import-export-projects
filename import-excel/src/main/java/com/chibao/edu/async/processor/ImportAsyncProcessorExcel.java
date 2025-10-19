package com.chibao.edu.async.processor;

import com.chibao.edu.common.ImportJob;
import com.chibao.edu.common.ImportOption;
import com.chibao.edu.common.RowResult;
import com.chibao.edu.entity.Patient;
import com.chibao.edu.repository.ImportJobRepository;
import com.chibao.edu.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ImportAsyncProcessorExcel {

    private final PatientRepository patientRepository;
    private final ImportJobRepository importJobRepository;
    private final Path errorReportDir = Paths.get(System.getProperty("java.io.tmpdir"), "excel-import-errors");

    @Async("importTaskExecutor")
    @Transactional
    public void processAsync(UUID jobId, List<RowResult> rows, ImportOption option) {
        ImportJob job = importJobRepository.findById(jobId).orElseThrow();
        initializeJob(job);

        List<RowResult> errorRows = new ArrayList<>();
        List<Patient> batch = new ArrayList<>();

        try {
            for (RowResult rr : rows) {
                if (rr.getErrors().isEmpty()) {
                    processRow(rr, option, batch, errorRows, job);
                } else {
                    errorRows.add(rr);
                    if (option == ImportOption.FAIL) {
                        failJob(job, jobId, errorRows);
                        return;
                    }
                }

                if (batch.size() >= 100) {
                    flushBatch(batch, job);
                }
            }

            flushBatch(batch, job);
            finalizeJob(job, jobId, errorRows);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            markJobFailed(job);
        }
    }

    /* ---------- Helper Methods ---------- */

    private void processRow(RowResult rr, ImportOption option, List<Patient> batch,
                            List<RowResult> errorRows, ImportJob job) {
        Optional<Patient> existing = patientRepository.findByNationalId(rr.getNationalId());

        if (existing.isPresent()) {
            handleDuplicate(rr, existing.get(), option, batch, errorRows, job);
        } else {
            batch.add(mapToPatient(rr));
        }
    }

    private void handleDuplicate(RowResult rr, Patient existing, ImportOption option,
                                 List<Patient> batch, List<RowResult> errorRows, ImportJob job) {
        switch (option) {
            case SKIP:
                // Do nothing
                break;
            case UPDATE:
                applyRowToPatient(rr, existing);
                batch.add(existing);
                break;
            case FAIL:
                rr.getErrors().add("Duplicate nationalId");
                errorRows.add(rr);
                failJob(job, job.getJobId(), errorRows);
                throw new RuntimeException("Import failed due to duplicate ID");
        }
    }

    private void initializeJob(ImportJob job) {
        job.setStatus("RUNNING");
        importJobRepository.save(job);
    }

    private void flushBatch(List<Patient> batch, ImportJob job) {
        if (batch.isEmpty()) return;
        patientRepository.saveAll(batch);
        batch.clear();
        updateJobProgress(job);
    }

    private void updateJobProgress(ImportJob job) {
        job.setStatus("RUNNING");
        importJobRepository.save(job);
    }

    private void failJob(ImportJob job, UUID jobId, List<RowResult> errorRows) {
        job.setStatus("FAILED");
        job.setCompletedAt(LocalDateTime.now());
        importJobRepository.save(job);

        try {
            writeErrors(jobId, errorRows);
        } catch (IOException ignored) {
        }
    }

    private void finalizeJob(ImportJob job, UUID jobId, List<RowResult> errorRows) throws IOException {
        job.setStatus(errorRows.isEmpty() ? "SUCCESS" : "PARTIAL_FAILED");
        job.setCompletedAt(LocalDateTime.now());

        if (!errorRows.isEmpty()) {
            String errorPath = writeErrors(jobId, errorRows);
            job.setErrorFile(errorPath);
        }

        importJobRepository.save(job);
    }

    private void markJobFailed(ImportJob job) {
        job.setStatus("FAILED");
        job.setCompletedAt(LocalDateTime.now());
        importJobRepository.save(job);
    }

    private Patient mapToPatient(RowResult rr) {
        Patient p = new Patient();
        applyRowToPatient(rr, p);
        return p;
    }

    private void applyRowToPatient(RowResult r, Patient p) {
        p.setFirstName(r.getFirstName());
        p.setLastName(r.getLastName());
        p.setEmail(r.getEmail());
        p.setPhone(r.getPhone());
        p.setNationalId(r.getNationalId());
        p.setDob(r.getDob());
    }

    private String writeErrors(UUID jobId, List<RowResult> errorRows) throws IOException {
        Files.createDirectories(errorReportDir);
        Path out = errorReportDir.resolve("errors-" + jobId + ".xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Errors");

            // Header row
            Row header = sheet.createRow(0);
            String[] headers = {"Row", "Errors", "First Name", "Last Name", "Email", "Phone", "National ID", "DOB"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            int rowIndex = 1;
            for (RowResult rr : errorRows) {
                Row row = sheet.createRow(rowIndex);
                row.createCell(0).setCellValue(rowIndex);
                row.createCell(1).setCellValue(String.join("; ", rr.getErrors()));
                row.createCell(2).setCellValue(rr.getFirstName());
                row.createCell(3).setCellValue(rr.getLastName());
                row.createCell(4).setCellValue(rr.getEmail());
                row.createCell(5).setCellValue(rr.getPhone());
                row.createCell(6).setCellValue(rr.getNationalId());
                row.createCell(7).setCellValue(rr.getDob() != null ? rr.getDob().toString() : "");
                rowIndex++;
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(out.toFile())) {
                workbook.write(fos);
            }
        }

        return out.toAbsolutePath().toString();
    }
}