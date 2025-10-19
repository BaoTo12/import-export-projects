package com.chibao.edu.async.processor;

import com.chibao.edu.common.ImportJob;
import com.chibao.edu.common.ImportOption;
import com.chibao.edu.common.RowResult;
import com.chibao.edu.entity.Patient;
import com.chibao.edu.repository.ImportJobRepository;
import com.chibao.edu.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
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

@Component
@RequiredArgsConstructor
public class ImportAsyncProcessorCSV {
    private final PatientRepository patientRepository;
    private final ImportJobRepository importJobRepository;
    Path errorReportDir = Paths.get(System.getProperty("java.io.tmpdir"), "excel-import-errors");
    // ? khi phương thức này được gọi qua proxy của Spring, chạy nó trên một thread từ
    // ? Executor có tên importTaskExecutor thay vì chạy trên thread hiện tại.
    /*
     * Spring triển khai @Async bằng proxy (AOP). Chỉ các cuộc gọi đi qua proxy mới kích hoạt async.
     * *Nếu bạn gọi processAsync(...) trực tiếp từ cùng 1 bean (ví dụ this.processAsync(...)
     * *hoặc gọi từ một method khác trong cùng class như startImportFromStoredFile), cuộc gọi sẽ
     * *không đi qua proxy → @Async sẽ không hoạt động → phương thức chạy đồng bộ trên cùng thread
     * *(bạn sẽ không thấy thread pool dùng).
     * *
     * **/
    @Async("importTaskExecutor")
    @Transactional
    public void processAsync(UUID jobId, List<RowResult> rows, ImportOption option) {
        ImportJob job = importJobRepository.findById(jobId).orElseThrow();
        initializeJob(job);

        List<String[]> errorCsvRows = new ArrayList<>();
        List<Patient> batch = new ArrayList<>();
        int success = 0;

        try {
            for (int i = 0; i < rows.size(); i++) {
                RowResult rr = rows.get(i);

                if (handleRowErrors(rr, errorCsvRows, i, option, job, jobId)) {
                    continue;
                }

                if (processExistingPatient(rr, option, errorCsvRows, i, batch, job, jobId)) {
                    success++;
                } else if (rr.getErrors().isEmpty() && !existsByNationalId(rr.getNationalId())) {
                    batch.add(mapToPatient(rr));
                }

                success += maybeFlushBatch(batch, job);
            }

            success += flushRemaining(batch);
            finalizeJob(job, jobId, errorCsvRows);

        } catch (Exception ex) {
            markJobFailed(job);
        }
    }

    /* ---------- Helper Methods ---------- */

    private void initializeJob(ImportJob job) {
        job.setStatus("RUNNING");
        importJobRepository.save(job);
    }

    private boolean handleRowErrors(RowResult rr, List<String[]> errorCsvRows, int rowIndex,
                                    ImportOption option, ImportJob job, UUID jobId) {
        if (rr.getErrors().isEmpty()) return false;

        errorCsvRows.add(toErrorRow(rowIndex + 1, rr.getErrors(), rr));
        if (option == ImportOption.FAIL) {
            failJob(job, jobId, errorCsvRows);
        }
        return true;
    }

    private boolean processExistingPatient(RowResult rr, ImportOption option,
                                           List<String[]> errorCsvRows, int rowIndex,
                                           List<Patient> batch, ImportJob job, UUID jobId) {
        Optional<Patient> existing = patientRepository.findByNationalId(rr.getNationalId());
        if (existing.isEmpty()) return false;

        switch (option) {
            case SKIP:
                return true; // skipped considered success
            case UPDATE:
                applyRowToPatient(rr, existing.get());
                batch.add(existing.get());
                return true;
            case FAIL:
                errorCsvRows.add(toErrorRow(rowIndex + 1, List.of("Duplicate nationalId"), rr));
                failJob(job, jobId, errorCsvRows);
                return false;
            default:
                return false;
        }
    }

    private boolean existsByNationalId(String nationalId) {
        return patientRepository.findByNationalId(nationalId).isPresent();
    }

    private int maybeFlushBatch(List<Patient> batch, ImportJob job) {
        final int batchSize = 100;
        if (batch.size() < batchSize) return 0;
        return flushBatch(batch, job);
    }

    private int flushBatch(List<Patient> batch, ImportJob job) {
        patientRepository.saveAll(batch);
        int size = batch.size();
        batch.clear();
        updateJobProgress(job);
        return size;
    }

    private int flushRemaining(List<Patient> batch) {
        if (batch.isEmpty()) return 0;
        patientRepository.saveAll(batch);
        int size = batch.size();
        batch.clear();
        return size;
    }

    private void updateJobProgress(ImportJob job) {
        job.setStatus("RUNNING");
        importJobRepository.save(job);
    }

    private void failJob(ImportJob job, UUID jobId, List<String[]> errorCsvRows) {
        job.setStatus("FAILED");
        job.setCompletedAt(LocalDateTime.now());
        importJobRepository.save(job);
        try {
            writeErrors(jobId, errorCsvRows);
        } catch (IOException ignored) {
        }
    }

    private void finalizeJob(ImportJob job, UUID jobId, List<String[]> errorCsvRows) throws IOException {
        job.setStatus(errorCsvRows.isEmpty() ? "SUCCESS" : "PARTIAL_FAILED");
        job.setCompletedAt(LocalDateTime.now());
        if (!errorCsvRows.isEmpty()) {
            String errorsPath = writeErrors(jobId, errorCsvRows);
            job.setErrorFile(errorsPath);
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

    private String writeErrors(UUID jobId, List<String[]> errorRows) throws IOException {
        Files.createDirectories(errorReportDir);
        Path out = errorReportDir.resolve("errors-" + jobId + ".csv");
        try (BufferedWriter writer = Files.newBufferedWriter(out);
             CSVPrinter printer = new CSVPrinter(writer,
                     CSVFormat.DEFAULT.withHeader("row", "errors", "firstName", "lastName",
                             "email", "phone", "nationalId", "dob"))) {
            for (String[] row : errorRows) printer.printRecord((Object[]) row);
        }
        return out.toAbsolutePath().toString();
    }

    private String[] toErrorRow(int rowNumber, List<String> errs, RowResult rr) {
        return new String[]{
                String.valueOf(rowNumber),
                String.join("; ", errs),
                rr.getFirstName(),
                rr.getLastName(),
                rr.getEmail(),
                rr.getPhone(),
                rr.getNationalId(),
                rr.getDob() == null ? "" : rr.getDob().toString()
        };
    }
}
