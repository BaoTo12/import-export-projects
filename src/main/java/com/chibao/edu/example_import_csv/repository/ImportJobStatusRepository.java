package com.chibao.edu.example_import_csv.repository;

import com.chibao.edu.example_import_csv.dto.ImportJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImportJobStatusRepository extends JpaRepository<ImportJobStatus, UUID> {}