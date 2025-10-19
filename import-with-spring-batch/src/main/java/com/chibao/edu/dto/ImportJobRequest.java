package com.chibao.edu.dto;

import com.chibao.edu.common.DuplicateStrategy;
import jakarta.validation.constraints.NotNull;

public record ImportJobRequest(
        @NotNull(message = "Duplicate strategy is required")
        DuplicateStrategy duplicateStrategy
) {}