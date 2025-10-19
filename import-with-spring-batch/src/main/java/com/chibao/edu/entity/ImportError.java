package com.chibao.edu.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportError {
    private Integer rowNumber;
    private String field;
    private String message;
    private String value;
}