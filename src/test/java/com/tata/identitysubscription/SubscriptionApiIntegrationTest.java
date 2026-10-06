package com.tata.identitysubscription;

import com.tata.identitysubscription.domain.model.aggregates.Account;
import com.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import com.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.tata.identitysubscription.domain.model.valueobjects.SubscriptionStatus;
import com.tata.identitysubscription.domain.repositories.AccountRepository;
import com.tata.identitysubscription.domain.services.PlanCatalog;
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

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SubscriptionApiIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired AccountRepository accounts;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void catalogExposesConfiguredPlansAndCapabilities() throws Exception {
        mvc.perform(get("/api/v1/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("ESSENTIAL"))
                .andExpect(jsonPath("$[0].monthlyPrice").value(9.90))
                .andExpect(jsonPath("$[0].capabilities", hasItem("INTAKE_CONFIRMATION")))
                .andExpect(jsonPath("$[1].code").value("FAMILY"))
                .andExpect(jsonPath("$[1].capabilities", hasItem("FAMILY_MONITORING")))
                .andExpect(jsonPath("$[1].capabilities", hasItem("ADHERENCE_INSIGHTS")));
    }

    @Test
    void currentSubscriptionCanBeChangedAndCapabilitiesAreUpdated() throws Exception {
        String accountId = createActiveAccount();

        mvc.perform(get("/api/v1/accounts/{accountId}/subscription", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.plan.code").value("ESSENTIAL"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mvc.perform(put("/api/v1/accounts/{accountId}/subscription", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planCode\":\"FAMILY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.code").value("FAMILY"))
                .andExpect(jsonPath("$.plan.capabilities", hasItem("FAMILY_ALERTS")))
                .andExpect(jsonPath("$.plan.capabilities", hasItem("ADHERENCE_INSIGHTS")));

        mvc.perform(get("/api/v1/accounts/{accountId}/subscription", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.code").value("FAMILY"));
    }

    @Test
    void invalidPlanIsRejectedWithoutChangingCurrentSubscription() throws Exception {
        String accountId = createActiveAccount();

        mvc.perform(put("/api/v1/accounts/{accountId}/subscription", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planCode\":\"UNKNOWN\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("PLAN_NOT_FOUND"));

        mvc.perform(get("/api/v1/accounts/{accountId}/subscription", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.code").value("ESSENTIAL"));
    }

    private String createActiveAccount() {
        String accountId = UUID.randomUUID().toString();
        Account account = Account.rehydrate(
                accountId,
                "Diego Mendoza",
                new EmailAddress(accountId + "@example.com"),
                "password-hash",
                AccountStatus.ACTIVE,
                null,
                null,
                PlanCatalog.ESSENTIAL,
                SubscriptionStatus.ACTIVE,
                Instant.parse("2026-10-28T12:00:00Z")
        );
        accounts.save(account);
        return accountId;
    }
}
