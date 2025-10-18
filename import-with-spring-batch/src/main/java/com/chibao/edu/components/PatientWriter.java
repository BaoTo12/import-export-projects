package com.chibao.edu.components;

import com.chibao.edu.entity.Patient;
import com.chibao.edu.repository.PatientRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class PatientWriter implements ItemWriter<Patient> {
    private final PatientRepository patientRepository;
    public PatientWriter(PatientRepository patientRepository) { this.patientRepository = patientRepository; }


    @Override
    public void write(Chunk<? extends Patient> items) throws Exception {
        if (items == null || items.isEmpty()) return;
        patientRepository.saveAll(items.getItems());
    }
}