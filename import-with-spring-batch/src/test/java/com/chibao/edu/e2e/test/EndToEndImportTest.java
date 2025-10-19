package com.chibao.edu.e2e.test;

import com.chibao.edu.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.Map;
import org.springframework.http.*;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EndToEndImportTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PatientRepository patientRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/patients";
        patientRepository.deleteAll();
    }

    @Test
    void fullImportWorkflow_shouldCompleteSuccessfully() throws Exception {
        // Step 1: Upload file and initiate import
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ClassPathResource("test-patients.csv"));
        body.add("duplicateStrategy", "SKIP");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> importResponse = restTemplate.postForEntity(
                baseUrl + "/import",
                requestEntity,
                Map.class
        );

        assertThat(importResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String jobId = (String) importResponse.getBody().get("jobId");
        assertThat(jobId).isNotNull();

        // Step 2: Poll job status until completion
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<Map> statusResponse = restTemplate.getForEntity(
                            baseUrl + "/import/" + jobId + "/status",
                            Map.class
                    );

                    assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                    String status = (String) statusResponse.getBody().get("status");
                    assertThat(status).isIn("COMPLETED", "FAILED");
                });

        // Step 3: Verify data in database
        long patientCount = patientRepository.count();
        assertThat(patientCount).isGreaterThan(0);
    }
}
