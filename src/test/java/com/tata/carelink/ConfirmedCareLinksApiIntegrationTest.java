package com.tata.carelink;

import com.tata.carelink.domain.model.aggregates.*;
import com.tata.carelink.domain.model.valueobjects.*;
import com.tata.carelink.domain.repositories.*;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConfirmedCareLinksApiIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired CareLinkRepository links;
    @Autowired OlderAdultProfileRepository adults;
    MockMvc mvc;
    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).build(); }

    @Test void returnsOnlyConfirmedConsentedLinksForRequestedCaregiverWithoutSecrets() throws Exception {
        String caregiver = UUID.randomUUID().toString();
        var now = Instant.now();
        var adult = adults.save(OlderAdultProfile.register(caregiver,
                new OlderAdultBasicData("Ana", LocalDate.of(1950, 1, 1)), null, now));
        var link = CareLink.createPending(caregiver, adult.id(), LinkingCode.issue("123456", now, Duration.ofMinutes(15)), now);
        link.accept("123456", now);
        link.registerConsent(true, now);
        link.confirm(now);
        links.save(link);
        links.save(CareLink.createPending(caregiver, adult.id(), LinkingCode.issue("654321", now, Duration.ofMinutes(15)), now));
        mvc.perform(get("/api/v1/care-links").param("caregiverId", caregiver))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].olderAdultId").value(adult.id()))
                .andExpect(jsonPath("$[0].olderAdultName").value("Ana"))
                .andExpect(jsonPath("$[0].linkingCode").doesNotExist());
        mvc.perform(get("/api/v1/care-links").param("caregiverId", UUID.randomUUID().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/care-links").param("caregiverId", " ")).andExpect(status().isBadRequest());
    }
}
