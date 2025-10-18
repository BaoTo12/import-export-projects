package com.chibao.edu.example_import_csv.service.impl;

import com.chibao.edu.example_import_csv.common.ImportOption;
import com.chibao.edu.example_import_csv.common.ParseResult;
import com.chibao.edu.example_import_csv.common.Status;
import com.chibao.edu.example_import_csv.dto.ImportJobStatus;
import com.chibao.edu.example_import_csv.dto.ImportJobStatusDto;
import com.chibao.edu.example_import_csv.dto.ImportPreviewResponse;
import com.chibao.edu.example_import_csv.dto.RowResult;
import com.chibao.edu.example_import_csv.entity.Patient;
import com.chibao.edu.example_import_csv.parser.CsvPatientParser;
import com.chibao.edu.example_import_csv.repository.ImportJobStatusRepository;
import com.chibao.edu.example_import_csv.repository.PatientRepository;
import com.chibao.edu.example_import_csv.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImportServiceImpl implements ImportService {

    private final CsvPatientParser parser;
    private final ImportJobStatusRepository jobRepo;
    private final PatientRepository patientRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final Path errorReportDir = Paths.get("uploads/errors"); // ensure exists


    @Override
    public ImportPreviewResponse preview(InputStream is) throws IOException {
        ParseResult pr = parser.parse(is);
        UUID jobId = UUID.randomUUID();
        ImportJobStatus job = new ImportJobStatus();
        job.setId(jobId);
        job.setStatus(Status.PENDING);
        job.setCreatedAt(Instant.now());
        job.setTotalRows(pr.getRows().size());
        jobRepo.save(job);

        // return first 20 rows as preview
        List<RowResult> previewRows = pr.getRows().stream().limit(20).collect(Collectors.toList());
        return ImportPreviewResponse.of(jobId, pr.isHeaderValid(), previewRows, pr.getMessage());
    }

    @Override
    public UUID startImport(InputStream is, ImportOption option) throws IOException {
        ParseResult pr = parser.parse(is);
        if (!pr.isHeaderValid()) {
            throw new IllegalArgumentException("Invalid headers: " + pr.getMessage());
        }
        if (pr.isTooManyRows()) {
            throw new IllegalArgumentException("Too many rows");
        }
        UUID jobId = UUID.randomUUID();
        ImportJobStatus job = new ImportJobStatus();
        job.setId(jobId);
        job.setStatus(Status.PENDING);
        job.setCreatedAt(Instant.now());
        job.setTotalRows(pr.getRows().size());
        jobRepo.save(job);

        // start async worker
        processAsync(jobId, pr.getRows(), option);
        return jobId;
    }

    @Async("importTaskExecutor")
    public void processAsync(UUID jobId, List<RowResult> rows, ImportOption option) {
        ImportJobStatus job = jobRepo.findById(jobId).orElseThrow();
        job.setStatus(Status.RUNNING);
        jobRepo.save(job);

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
                        job.setStatus(Status.FAILED);
                        job.setFailedCount(failed);
                        jobRepo.save(job);
                        writeErrors(jobId, errorCsvRows);
                        return;
                    }
                    continue;
                }

                Optional<Patient> existing = patientRepo.findByNationalId(rr.getNationalId());
                if (existing.isPresent()) {
                    switch (option) {
                        case SKIP:
                            // do nothing
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
                            job.setStatus(Status.FAILED);
                            job.setFailedCount(failed);
                            jobRepo.save(job);
                            writeErrors(jobId, errorCsvRows);
                            return;
                    }
                } else {
                    Patient pNew = new Patient();
                    applyRowToPatient(rr, pNew);
                    toSave.add(pNew);
                }

                // flush batch
                if (toSave.size() >= batchSize) {
                    patientRepo.saveAll(toSave);
                    patientRepo.flush(); // if using JpaRepository with flush
                    success += toSave.size();
                    toSave.clear();
                    job.setSuccessCount(success);
                    job.setFailedCount(failed);
                    jobRepo.save(job);
                }
            }

            // flush remaining
            if (!toSave.isEmpty()) {
                patientRepo.saveAll(toSave);
                patientRepo.flush();
                success += toSave.size();
            }

            // finalize
            job.setSuccessCount(success);
            job.setFailedCount(failed);
            job.setStatus(Status.SUCCESS);
            String errorsPath = null;
            if (!errorCsvRows.isEmpty()) {
                errorsPath = writeErrors(jobId, errorCsvRows);
                job.setErrorReportPath(errorsPath);
                job.setStatus(Status.FAILED); // if any failed, depending on acceptance you may mark partial success — adapt
            }
            jobRepo.save(job);

        } catch (Exception ex) {
            job.setStatus(Status.FAILED);
            job.setMessage(ex.getMessage());
            jobRepo.save(job);
            // log exception
        }
    }

    private String writeErrors(UUID jobId, List<String[]> errorRows) throws IOException {
        Files.createDirectories(errorReportDir);
        Path out = errorReportDir.resolve("errors-" + jobId + ".csv");
        try (BufferedWriter writer = Files.newBufferedWriter(out);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("row", "errors", "firstName", "lastName", "email", "phone", "nationalId", "dob"))) {
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

    @Override
    public ImportJobStatusDto getStatus(UUID jobId) {
        return jobRepo.findById(jobId)
                .map(j -> ImportJobStatusDto.from(j))
                .orElse(null);
    }

    @Override
    public Resource getErrorReport(UUID jobId) {
        return jobRepo.findById(jobId)
                .map(ImportJobStatus::getErrorReportPath)
                .filter(Objects::nonNull)
                .map(p -> {
                    try {
                        return new FileSystemResource(p);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .orElse(null);
    }
}