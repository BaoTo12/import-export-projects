package com.chibao.edu.dto;

import com.chibao.edu.common.RowResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportPreviewResponse {
    private UUID jobId;
    private boolean headerValid;
    private boolean tooManyRows;
    private int totalRows;
    private List<RowResult> previewRows;
    private String message;


    public static ImportPreviewResponse of(UUID jobId, boolean headerValid, List<RowResult> rows, String message) {
        ImportPreviewResponse resp = new ImportPreviewResponse();
        resp.setJobId(jobId);
        resp.setHeaderValid(headerValid);
        resp.setPreviewRows(rows);
        resp.setTotalRows(rows != null ? rows.size() : 0);
        resp.setMessage(message);
        resp.setTooManyRows(false);
        return resp;
    }


    public static ImportPreviewResponse error(String message) {
        ImportPreviewResponse resp = new ImportPreviewResponse();
        resp.setHeaderValid(false);
        resp.setMessage(message);
        return resp;
    }


    public static ImportPreviewResponse started(UUID jobId) {
        ImportPreviewResponse resp = new ImportPreviewResponse();
        resp.setJobId(jobId);
        resp.setHeaderValid(true);
        resp.setMessage("Import started");
        return resp;
    }
}