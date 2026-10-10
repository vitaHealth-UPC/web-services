package com.tata.identitysubscription;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = "tata.demo.seed=true")
@ActiveProfiles("test")
class DemoSeedIntegrationTest {
  private static final String CAREGIVER_ID = "d3a10000-0000-4000-8000-000000000001";
  private static final String OLDER_ADULT_ID = "d3a10000-0000-4000-8000-000000000002";

  @Autowired WebApplicationContext context;
  MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void demoCaregiverAndOlderAdultCanSignInAndSeeTheCareLink() throws Exception {
    var session =
        mvc.perform(
                post("/api/v1/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"demo.caregiver@tata.app\",\"password\":\"Tata-Demo-2026\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value(CAREGIVER_ID))
            .andReturn();
    var token = JsonPath.<String>read(session.getResponse().getContentAsString(), "$.accessToken");

    mvc.perform(
            get("/api/v1/care-links")
                .param("caregiverId", CAREGIVER_ID)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].olderAdultName").value("Rosa Vargas"));

    mvc.perform(
            post("/api/v1/pin-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"olderAdultId\":\"" + OLDER_ADULT_ID + "\",\"pin\":\"1234\"}"))
        .andExpect(status().isOk());
  }
}
