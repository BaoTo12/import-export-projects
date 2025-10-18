package com.chibao.edu.writers;

import com.chibao.edu.models.Patient;
import com.chibao.edu.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientWriter implements ItemWriter<Patient> {

    private final PatientRepository patientRepository;

    @Override
    public void write(Chunk chunk) throws Exception {
        patientRepository.saveAll(chunk.getItems());
        log.info("Saved batch of {} patients", chunk.size());
    }
}
