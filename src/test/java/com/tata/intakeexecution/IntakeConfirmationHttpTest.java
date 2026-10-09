package com.tata.intakeexecution;
import com.tata.identitysubscription.application.commandservices.PinCommandService;
import com.tata.identitysubscription.domain.model.commands.RegisterPinCommand;
import com.tata.identitysubscription.domain.model.commands.AuthenticateWithPinCommand;
import com.tata.intakeexecution.domain.model.aggregates.Intake;
import com.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @ActiveProfiles("test")
class IntakeConfirmationHttpTest {
 @Autowired WebApplicationContext context;
 @Autowired PinCommandService pins;
 @Autowired IntakeRepository repository;
 @Autowired com.tata.carelink.application.commandservices.CareLinkCommandService links;
 @Autowired com.tata.identitysubscription.domain.repositories.AccountRepository accounts;
 @Autowired com.tata.identitysubscription.application.internal.outboundservices.PasswordHasher hasher;
 @Test void retryResponseMarksTheExistingConfirmationWithoutChangingItsTimestamp() throws Exception {
  var account=com.tata.identitysubscription.domain.model.aggregates.Account.register("Caregiver",new com.tata.identitysubscription.domain.model.valueobjects.EmailAddress(UUID.randomUUID()+"@example.com"),hasher.hash("password123"),hasher.hash("123456"),Instant.now(),java.time.Duration.ofMinutes(15));
  account.completeVerification();
  account=accounts.save(account);
  var owner=links.registerOlderAdult(new com.tata.carelink.domain.model.commands.RegisterOlderAdultProfileCommand(account.id(),"Ana",java.time.LocalDate.of(1950,1,1),null,null,null)).id();
  var link=links.generateLinkingCode(new com.tata.carelink.domain.model.commands.GenerateLinkingCodeCommand(account.id(),owner));
  links.accept(new com.tata.carelink.domain.model.commands.AcceptCareLinkCommand(account.id(),link.linkingCode()));
  links.registerConsent(new com.tata.carelink.domain.model.commands.RegisterConsentCommand(link.id(),true));
  pins.register(new RegisterPinCommand(owner,"1234"));
  var token=pins.authenticate(new AuthenticateWithPinCommand(owner,"1234"));
  var intake=repository.saveAll(List.of(Intake.createScheduled(UUID.randomUUID().toString(), UUID.randomUUID().toString(), owner,
    new MedicationSnapshot("Losartán","1 comprimido","Con agua"),Instant.now().plusSeconds(60),Instant.now()))).getFirst();
  var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  var first=mvc.perform(post("/api/v1/intakes/"+intake.id()+"/confirmation").header("Authorization","Bearer "+token.accessToken())
    .contentType(MediaType.APPLICATION_JSON).content("{\"channel\":\"TOUCH\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.alreadyConfirmed").value(false)).andReturn().getResponse().getContentAsString();
  String recorded=com.jayway.jsonpath.JsonPath.read(first,"$.confirmedAt");
  mvc.perform(post("/api/v1/intakes/"+intake.id()+"/confirmation").header("Authorization","Bearer "+token.accessToken())
    .contentType(MediaType.APPLICATION_JSON).content("{\"channel\":\"TOUCH\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.alreadyConfirmed").value(true)).andExpect(jsonPath("$.confirmedAt").value(recorded));
 }
}
