package com.chibao.edu.utils;

import com.chibao.edu.entity.ImportError;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ValidationResult<T> {
    private boolean valid;
    private T data;
    private List<T> validData;
    private List<ImportError> errors;
}
