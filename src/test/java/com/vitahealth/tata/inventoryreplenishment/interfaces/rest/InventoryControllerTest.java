package com.vitahealth.tata.inventoryreplenishment.interfaces.rest;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
import com.vitahealth.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.commands.GenerateLinkingCodeCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterConsentCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterOlderAdultProfileCommand;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Exercises inventory HTTP contracts with authenticated ownership and real consented care links.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InventoryControllerTest {

  @Autowired WebApplicationContext context;
  @Autowired MedicationRepository medications;
  @Autowired AccountRepository accounts;
  @Autowired SessionTokenService tokens;
  @Autowired CareLinkCommandService links;
  private String adultId;
  private MockMvc mockMvc;

  @Test
  void rejectsInitialInventoryForInactiveMedication() throws Exception {
    var medication = Medication.register(adultId, "Losartan", "Tablet", Instant.now());
    medication.deactivate();
    medications.save(medication);
    mockMvc
        .perform(
            post("/api/v1/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(initialInventory(medication.id(), 10, 2)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("MEDICATION_INACTIVE"));
  }

  @Test
  void rejectsInventoryForUnknownMedication() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(initialInventory(UUID.randomUUID().toString(), 10, 2)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("RESOURCE_ACCESS_DENIED"));
  }

  @BeforeEach
  void setUp() {
    String caregiverId = UUID.randomUUID().toString();
    accounts.save(
        Account.rehydrate(
            caregiverId,
            "Caregiver",
            new EmailAddress(caregiverId + "@example.com"),
            "hash",
            AccountStatus.ACTIVE,
            null,
            null));
    var profile =
        links
            .registerOlderAdult(
                new RegisterOlderAdultProfileCommand(
                    caregiverId, "Rosa", LocalDate.of(1950, 1, 1), "Diego", "Hijo", "+51999999999"))
            .toOptional()
            .orElseThrow();
    adultId = profile.id();
    var generated =
        links
            .generateLinkingCode(new GenerateLinkingCodeCommand(caregiverId, adultId))
            .toOptional()
            .orElseThrow();
    var accepted =
        links
            .accept(new AcceptCareLinkCommand(caregiverId, generated.linkingCode()))
            .toOptional()
            .orElseThrow();
    links
        .registerConsent(new RegisterConsentCommand(accepted.id(), true))
        .toOptional()
        .orElseThrow();
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .defaultRequest(
                get("/")
                    .header("Authorization", "Bearer " + tokens.issue(caregiverId).accessToken()))
            .build();
  }

  @Test
  void registersInitialInventory() throws Exception {
    var medicationId = newId();

    mockMvc
        .perform(
            post("/api/v1/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(initialInventory(medicationId, 30, 5)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.medicationId").value(medicationId))
        .andExpect(jsonPath("$.remainingStock").value(30))
        .andExpect(jsonPath("$.replenishmentThreshold").value(5))
        .andExpect(jsonPath("$.lowStock").value(false))
        .andExpect(jsonPath("$.batches.length()").value(1))
        .andExpect(jsonPath("$.batches[0].quantity").value(30));
  }

  @Test
  void rejectsMissingFields() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"medicationId\":\"" + newId() + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Invalid request data"));
  }

  @Test
  void rejectsMalformedBody() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void rejectsInvalidQuantityWithEnglishMessageByDefault() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(initialInventory(newId(), 0, 5)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_QUANTITY"))
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Quantities must be positive and the replenishment threshold cannot be"
                        + " negative"));
  }

  @Test
  void localizesMessageForLatinAmericanSpanishButKeepsCode() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/inventories")
                .header("Accept-Language", "es-419")
                .contentType(MediaType.APPLICATION_JSON)
                .content(initialInventory(newId(), 10, -1)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_QUANTITY"))
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Las cantidades deben ser positivas y el umbral de reposición no puede ser"
                        + " negativo"));
  }

  @Test
  void rejectsSecondInventoryForSameMedication() throws Exception {
    var medicationId = newId();
    registerInventory(medicationId, 30, 5);

    mockMvc
        .perform(
            post("/api/v1/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(initialInventory(medicationId, 10, 2)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVENTORY_ALREADY_EXISTS"));
  }

  @Test
  void returnsRemainingStockWithLowStockFlag() throws Exception {
    var medicationId = newId();
    registerInventory(medicationId, 4, 5);

    mockMvc
        .perform(get("/api/v1/inventories/{medicationId}", medicationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.remainingStock").value(4))
        .andExpect(jsonPath("$.lowStock").value(true));
  }

  @Test
  void returnsNotFoundForUnknownMedication() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/inventories/{medicationId}", newId()).header("Accept-Language", "es-419"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("INVENTORY_NOT_FOUND"))
        .andExpect(
            jsonPath("$.message").value("No hay inventario registrado para este medicamento"));
  }

  @Test
  void registersReplenishment() throws Exception {
    var medicationId = newId();
    registerInventory(medicationId, 4, 5);

    mockMvc
        .perform(
            post("/api/v1/inventories/{medicationId}/replenishments", medicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":20}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.remainingStock").value(24))
        .andExpect(jsonPath("$.lowStock").value(false))
        .andExpect(jsonPath("$.batches.length()").value(2));
  }

  @Test
  void rejectsInvalidReplenishmentQuantity() throws Exception {
    var medicationId = newId();
    registerInventory(medicationId, 4, 5);

    mockMvc
        .perform(
            post("/api/v1/inventories/{medicationId}/replenishments", medicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":-5}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_QUANTITY"));
  }

  @Test
  void replenishmentRequiresExistingInventory() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/inventories/{medicationId}/replenishments", newId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":20}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("INVENTORY_NOT_FOUND"));
  }

  @Test
  void documentsInventoryOperationsInOpenApi() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/inventories")))
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/inventories/{medicationId}")))
        .andExpect(
            jsonPath("$.paths", hasKey("/api/v1/inventories/{medicationId}/replenishments")));
  }

  private void registerInventory(String medicationId, int quantity, int threshold)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(initialInventory(medicationId, quantity, threshold)))
        .andExpect(status().isCreated());
  }

  private static String initialInventory(String medicationId, int quantity, int threshold) {
    return "{\"medicationId\":\"%s\",\"initialQuantity\":%d,\"replenishmentThreshold\":%d}"
        .formatted(medicationId, quantity, threshold);
  }

  private String newId() {
    return medications.save(Medication.register(adultId, "Losartan", "Tablet", Instant.now())).id();
  }

  @Test
  void rejectsReadingAndWritingAnotherAdultsInventory() throws Exception {
    String otherMedication =
        medications
            .save(
                Medication.register(
                    UUID.randomUUID().toString(), "Private medication", "Tablet", Instant.now()))
            .id();
    mockMvc
        .perform(get("/api/v1/inventories/{medicationId}", otherMedication))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/v1/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(initialInventory(otherMedication, 10, 2)))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/v1/inventories/{medicationId}/replenishments", otherMedication)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":20}"))
        .andExpect(status().isForbidden());
  }
}
