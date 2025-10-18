package com.chibao.edu.example_import_csv.common;

import com.chibao.edu.example_import_csv.dto.RowResult;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ParseResult {
    private boolean headerValid;
    private List<RowResult> rows;
    private String message;
    private boolean tooManyRows;
}