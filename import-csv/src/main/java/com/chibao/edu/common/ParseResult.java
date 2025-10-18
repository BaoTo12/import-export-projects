package com.chibao.edu.common;

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


    public static ParseResult error(String message) {
        return new ParseResult(false, List.of(), message, false);
    }


    public static ParseResult tooManyRows(String message) {
        return new ParseResult(false, List.of(), message, true);
    }
}