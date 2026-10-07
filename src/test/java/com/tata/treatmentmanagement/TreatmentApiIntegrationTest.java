package com.tata.treatmentmanagement;

import com.tata.treatmentmanagement.domain.services.ICareLinkVerificationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Treatment lifecycle through the real context on H2. Any caregiver except "intruder" has a care link. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TreatmentApiIntegrationTest {
    private static final String CAREGIVER = "caregiver-1";

    @TestConfiguration
    static class CareLinkStub {
        @Bean
        @Primary
        ICareLinkVerificationPort allowEveryoneButTheIntruder() {
            return (caregiverId, olderAdultId) -> !"intruder".equals(caregiverId);
        }
    }

    @Autowired WebApplicationContext context;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private static String newAdult() {
        return UUID.randomUUID().toString();
    }

    private String json(String path, String body) throws Exception {
        var response = mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }

    private String registerMedication(String adult, String name) throws Exception {
        return json("/api/v1/older-adults/" + adult + "/medications",
                "{\"caregiverId\":\"" + CAREGIVER + "\",\"name\":\"" + name + "\",\"presentation\":\"50 mg\"}");
    }

    private String createTreatment(String adult, String name) throws Exception {
        return json("/api/v1/older-adults/" + adult + "/treatments",
                "{\"caregiverId\":\"" + CAREGIVER + "\",\"name\":\"" + name + "\"}");
    }

    private void configure(String treatmentId, String medicationId) throws Exception {
        mockMvc.perform(put("/api/v1/treatments/" + treatmentId + "/regimen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caregiverId\":\"" + CAREGIVER + "\",\"medicationId\":\"" + medicationId
                                + "\",\"dose\":\"1 comprimido\",\"frequency\":\"DAILY\","
                                + "\"scheduledTimes\":[\"08:00:00\",\"20:00:00\"],\"instructions\":\"Con agua\","
                                + "\"reminderLeadMinutes\":10}"))
                .andExpect(status().isOk());
    }

    @Test
    void medicationsAreListedByOlderAdult() throws Exception {
        var adult = newAdult();
        registerMedication(adult, "Losartán");
        registerMedication(adult, "Atorvastatina");
        registerMedication(newAdult(), "Metformina");

        mockMvc.perform(get("/api/v1/older-adults/" + adult + "/medications").param("caregiverId", CAREGIVER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Atorvastatina"))
                .andExpect(jsonPath("$[1].name").value("Losartán"));
    }

    @Test
    void anIncompleteTreatmentCannotBeActivated() throws Exception {
        var treatmentId = createTreatment(newAdult(), "Presión");

        mockMvc.perform(post("/api/v1/treatments/" + treatmentId + "/activation").param("caregiverId", CAREGIVER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("INCOMPLETE_TREATMENT"));
    }

    @Test
    void aTreatmentGoesFromDraftToActiveToPausedAndBack() throws Exception {
        var adult = newAdult();
        var medicationId = registerMedication(adult, "Losartán");
        var treatmentId = createTreatment(adult, "Presión");
        configure(treatmentId, medicationId);

        mockMvc.perform(post("/api/v1/treatments/" + treatmentId + "/activation").param("caregiverId", CAREGIVER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(post("/api/v1/treatments/" + treatmentId + "/pause").param("caregiverId", CAREGIVER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"))
                .andExpect(jsonPath("$.scheduledTimes", hasSize(2)));
        mockMvc.perform(post("/api/v1/treatments/" + treatmentId + "/resume").param("caregiverId", CAREGIVER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/older-adults/" + adult + "/treatments").param("caregiverId", CAREGIVER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].medicationId").value(medicationId));
    }

    @Test
    void deactivatingTheMedicationPausesItsActiveTreatmentAndBlocksResuming() throws Exception {
        var adult = newAdult();
        var medicationId = registerMedication(adult, "Losartán");
        var treatmentId = createTreatment(adult, "Presión");
        configure(treatmentId, medicationId);
        mockMvc.perform(post("/api/v1/treatments/" + treatmentId + "/activation").param("caregiverId", CAREGIVER))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/medications/" + medicationId + "/deactivation").param("caregiverId", CAREGIVER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/v1/treatments/" + treatmentId).param("caregiverId", CAREGIVER))
                .andExpect(jsonPath("$.status").value("PAUSED"));
        mockMvc.perform(post("/api/v1/treatments/" + treatmentId + "/resume").param("caregiverId", CAREGIVER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("MEDICATION_INACTIVE"));
    }

    @Test
    void aCaregiverWithoutCareLinkIsRejectedEverywhere() throws Exception {
        var adult = newAdult();

        mockMvc.perform(post("/api/v1/older-adults/" + adult + "/medications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caregiverId\":\"intruder\",\"name\":\"Losartán\",\"presentation\":\"50 mg\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("CARE_LINK_NOT_AUTHORIZED"));
        mockMvc.perform(get("/api/v1/older-adults/" + adult + "/treatments").param("caregiverId", "intruder"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/older-adults/" + adult + "/medications").param("caregiverId", "intruder"))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingFieldsAreRejected() throws Exception {
        mockMvc.perform(post("/api/v1/older-adults/" + newAdult() + "/medications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caregiverId\":\"" + CAREGIVER + "\",\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"));
    }

    @Test
    void unknownTreatmentIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/treatments/" + UUID.randomUUID()).param("caregiverId", CAREGIVER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("TREATMENT_NOT_FOUND"));
    }
}
