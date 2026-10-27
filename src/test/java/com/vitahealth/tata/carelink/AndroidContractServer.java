package com.vitahealth.tata.carelink;

import com.vitahealth.tata.TataApplication;
import com.vitahealth.tata.familymonitoring.application.internal.queryservices.FamilyMonitorSelectionService;
import com.vitahealth.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import com.vitahealth.tata.identitysubscription.application.internal.commandservices.RequestPasswordRecoveryCommandHandler;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationCodeGenerator;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationPayloadCipher;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.hashing.PasswordHasher;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RequestPasswordRecoveryCommand;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.VerificationDeliveryJpaRepository;
import java.nio.file.*;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.web.context.WebApplicationContext;

/** Explicit local-only HTTP host for the Android contract probe; never loaded by production. */
public final class AndroidContractServer {
  private AndroidContractServer() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 1) throw new IllegalArgumentException("Supply a private local fixture path");
    var destination = Path.of(args[0]).toAbsolutePath();
    if (Files.exists(destination))
      throw new IllegalArgumentException("Fixture path already exists");
    try (var context =
        new SpringApplication(TataApplication.class, RecoveryProbeConfiguration.class)
            .run(
                "--spring.profiles.active=test", "--server.port=0", "--server.address=127.0.0.1")) {
      var fixture = CareLinkHttpFixture.approved((WebApplicationContext) context);
      var monitor =
          context
              .getBean(FamilyMonitorSelectionService.class)
              .requireCurrent(fixture.olderAdultId(), fixture.caregiverId());
      var now = Instant.now();
      monitor.addAlert(
          UUID.randomUUID().toString(),
          "Local probe medication",
          now.minusSeconds(60),
          "Omitted",
          now);
      var alertId =
          context
              .getBean(IFamilyMonitorRepository.class)
              .save(monitor)
              .getAlerts()
              .getFirst()
              .getId();
      var properties = new Properties();
      properties.setProperty(
          "baseUrl",
          "http://127.0.0.1:"
              + context.getBean(Environment.class).getProperty("local.server.port")
              + "/");
      properties.setProperty("adultId", fixture.olderAdultId());
      properties.setProperty("caregiverId", fixture.caregiverId());
      properties.setProperty("adultToken", fixture.adultToken());
      properties.setProperty("caregiverToken", fixture.caregiverToken());
      properties.setProperty("alertId", alertId.toString());
      if (Boolean.getBoolean("tata.probe.recovery")) {
        var accounts = context.getBean(AccountRepository.class);
        var account = accounts.findById(fixture.caregiverId()).orElseThrow();
        var hasher = context.getBean(PasswordHasher.class);
        account.replacePasswordHash(hasher.hash("old-probe-password"));
        accounts.save(account);
        var request = context.getBean(RequestPasswordRecoveryCommandHandler.class);
        request
            .handle(new RequestPasswordRecoveryCommand(account.email().value()))
            .toOptional()
            .orElseThrow();
        var delivery =
            context.getBean(VerificationDeliveryJpaRepository.class).findAll().stream()
                .filter(row -> row.accountId().equals(account.id()))
                .findFirst()
                .orElseThrow();
        var cipher = context.getBean(VerificationPayloadCipher.class);
        properties.setProperty("recoveryEmail", account.email().value());
        properties.setProperty(
            "recoveryCode", cipher.decrypt(delivery.id(), delivery.encryptedCode()));
      }
      try (var output = Files.newOutputStream(destination, StandardOpenOption.CREATE_NEW)) {
        properties.store(output, "Ephemeral local integration credentials; do not publish");
      }
      System.out.println("ANDROID_CONTRACT_SERVER_READY (credentials withheld)");
      var stop = destination.resolveSibling(destination.getFileName() + ".done");
      var deadline = Instant.now().plusSeconds(900);
      while (!Files.exists(stop) && Instant.now().isBefore(deadline)) Thread.sleep(500);
    } finally {
      Files.deleteIfExists(destination);
      Files.deleteIfExists(destination.resolveSibling(destination.getFileName() + ".done"));
    }
  }

  /** A per-process code source lets the isolated probe read its mailbox without external email. */
  @TestConfiguration
  @ConditionalOnProperty(name = "tata.probe.recovery", havingValue = "true")
  static class RecoveryProbeConfiguration {
    @Bean
    @Primary
    VerificationCodeGenerator probeRecoveryCode() {
      var code = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));
      return () -> code;
    }
  }
}
