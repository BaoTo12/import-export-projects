package com.chibao.edu.repository;

import com.chibao.edu.entity.ImportJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImportJobStatusRepository extends JpaRepository<ImportJobStatus, UUID> {
}
