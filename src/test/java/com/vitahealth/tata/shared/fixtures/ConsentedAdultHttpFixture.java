package com.vitahealth.tata.shared.fixtures;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

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
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Creates real persisted ownership and adult credentials for protected HTTP integration tests. */
public record ConsentedAdultHttpFixture(String owner, MockMvc mvc) {
  /** Executes real account, relationship and session contracts without bypassing authorization. */
  public static ConsentedAdultHttpFixture create(WebApplicationContext context) {
    var caregiver = UUID.randomUUID().toString();
    context
        .getBean(AccountRepository.class)
        .save(
            Account.rehydrate(
                caregiver,
                "Caregiver",
                new EmailAddress(caregiver + "@example.com"),
                "hash",
                AccountStatus.ACTIVE,
                null,
                null));
    var links = context.getBean(CareLinkCommandService.class);
    var profile =
        links
            .registerOlderAdult(
                new RegisterOlderAdultProfileCommand(
                    caregiver, "Rosa", LocalDate.of(1950, 1, 1), null, null, null))
            .toOptional()
            .orElseThrow();
    var generated =
        links
            .generateLinkingCode(new GenerateLinkingCodeCommand(caregiver, profile.id()))
            .toOptional()
            .orElseThrow();
    var accepted =
        links
            .accept(new AcceptCareLinkCommand(caregiver, generated.linkingCode()))
            .toOptional()
            .orElseThrow();
    links
        .registerConsent(new RegisterConsentCommand(accepted.id(), true))
        .toOptional()
        .orElseThrow();
    var token =
        context.getBean(SessionTokenService.class).issueOlderAdult(profile.id()).accessToken();
    var mvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .defaultRequest(get("/").header("Authorization", "Bearer " + token))
            .build();
    return new ConsentedAdultHttpFixture(profile.id(), mvc);
  }
}
