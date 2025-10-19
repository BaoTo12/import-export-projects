package com.chibao.edu.writers;

import com.chibao.edu.entity.Patient;
import com.chibao.edu.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class PatientItemWriter implements ItemWriter<Patient> {

    private final PatientRepository patientRepository;

    @Override
    public void write(Chunk<? extends Patient> chunk) throws Exception {
        List<? extends Patient> patients = chunk.getItems();

        if (patients.isEmpty()) {
            return;
        }

        patientRepository.saveAll(patients);
        log.debug("Saved batch of {} patients", patients.size());
    }
}