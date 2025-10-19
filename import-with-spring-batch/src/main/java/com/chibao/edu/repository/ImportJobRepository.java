package com.chibao.edu.repository;

import com.chibao.edu.common.ImportJobStatus;
import com.chibao.edu.entity.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, String> {
    Optional<ImportJob> findByBatchJobExecutionId(Long batchJobExecutionId);
    Optional<ImportJob> findByIdAndStatus(String id, ImportJobStatus status);
}
