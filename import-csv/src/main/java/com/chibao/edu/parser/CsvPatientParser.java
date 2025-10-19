package com.chibao.edu.parser;

import com.chibao.edu.common.FileParser;
import com.chibao.edu.common.ParseResult;
import com.chibao.edu.common.RowResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class CsvPatientParser implements FileParser {
    private static final int MAX_ROWS = 1000;
    private static final List<String> EXPECTED_HEADERS = List.of("firstName","lastName","email","phone","nationalId","dob");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".csv");
    }

    @Override
    public ParseResult parse(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) throw new IOException("File not found: " + filePath);

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()                 // tells parser to use first record as header
                .setSkipHeaderRecord(true)   // skip the header row during iteration
                .setTrim(true)               // trim whitespace
                .build();

        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format)) {

            HeaderValidationResult headerResult = validateHeaders(parser.getHeaderMap());
            if (!headerResult.valid()) {
                return new ParseResult(false, Collections.emptyList(), headerResult.message(), false);
            }

            List<RowResult> rows = parseRecords(parser);
            return new ParseResult(true, rows, null, false);
        }
    }

    private HeaderValidationResult validateHeaders(Map<String, Integer> headerMap) {
        List<String> headers = headerMap.keySet().stream().map(String::toLowerCase).toList();
        List<String> missing = EXPECTED_HEADERS.stream()
                .filter(h -> !headers.contains(h.toLowerCase()))
                .collect(Collectors.toList());

        boolean valid = missing.isEmpty();
        String msg = valid ? null : "Missing headers: " + String.join(", ", missing);
        return new HeaderValidationResult(valid, msg);
    }

    private List<RowResult> parseRecords(CSVParser parser) {
        List<RowResult> rows = new ArrayList<>();
        int count = 0;

        for (CSVRecord record : parser) {
            count++;
            if (count > MAX_ROWS) {
                return List.of(RowResult.error("Too many rows. Max allowed: " + MAX_ROWS));
            }
            rows.add(parseRow(record));
        }
        return rows;
    }

    private RowResult parseRow(CSVRecord record) {
        RowResult rr = new RowResult();
        rr.setFirstName(get(record, "firstName"));
        rr.setLastName(get(record, "lastName"));
        rr.setEmail(get(record, "email"));
        rr.setPhone(get(record, "phone"));
        rr.setNationalId(get(record, "nationalId"));

        rr.setDob(parseDate(get(record, "dob")));
        rr.setErrors(validateRow(rr, get(record, "dob")));
        return rr;
    }

    private LocalDate parseDate(String rawDob) {
        if (rawDob == null || rawDob.isBlank()) return null;

        try {
            return LocalDate.parse(rawDob);
        } catch (DateTimeParseException e) {
            return parseDateLenient(rawDob);
        }
    }

    private List<String> validateRow(RowResult rr, String rawDob) {
        List<String> errs = new ArrayList<>();
        if (isBlank(rr.getFirstName())) errs.add("firstName required");
        if (isBlank(rr.getNationalId())) errs.add("nationalId required");
        if (notBlank(rr.getEmail()) && !isValidEmail(rr.getEmail())) errs.add("invalid email");
        if (rr.getDob() == null && notBlank(rawDob)) errs.add("dob not parseable");
        return errs;
    }

    private String get(CSVRecord record, String col) {
        try {
            return record.isSet(col) ? record.get(col).trim() : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private LocalDate parseDateLenient(String text) {
        List<String> patterns = List.of("d/M/yyyy", "d/M/yy", "dd/MM/yyyy", "dd-MM-yyyy", "MM/dd/yyyy");
        for (String pattern : patterns) {
            try {
                var fmt = java.time.format.DateTimeFormatter.ofPattern(pattern);
                return LocalDate.parse(text, fmt);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private boolean notBlank(String s) {
        return !isBlank(s);
    }

    private record HeaderValidationResult(boolean valid, String message) {

    }
}
