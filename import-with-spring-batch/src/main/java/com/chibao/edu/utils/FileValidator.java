package com.chibao.edu.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Component
public class FileValidator {

    @Value("${import.batch.max-rows}")
    private Integer maxRows;

    @Value("#{'${import.allowed-extensions}'.split(',')}")
    private List<String> allowedExtensions;

    public void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("File name is invalid");
        }

        String extension = getFileExtension(filename);
        if (!allowedExtensions.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                    String.format("File extension .%s is not supported. Allowed: %s",
                            extension, String.join(", ", allowedExtensions))
            );
        }

        long fileSizeInMB = file.getSize() / (1024 * 1024);
        if (fileSizeInMB > 10) {
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }
    }

    public String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
