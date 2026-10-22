package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.identitysubscription.application.commandservices.PinCommandService;
import com.vitahealth.tata.identitysubscription.domain.exceptions.DuplicatePinCredentialException;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.PinCredential;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RegisterPinCommand;
import com.vitahealth.tata.identitysubscription.domain.repositories.PinCredentialRepository;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Exercises the PIN uniqueness constraint and concurrent enrollment through real transactions. */
@SpringBootTest
@ActiveProfiles("test")
class PinEnrollmentConflictIntegrationTest {
  @Autowired PinCredentialRepository credentials;
  @Autowired PinCommandService commands;

  @Test
  void duplicateCredentialDoesNotReplaceTheWinner() {
    String adult = UUID.randomUUID().toString();
    var winner = credentials.save(PinCredential.register(adult, "encoded-original"));
    assertThrows(
        DuplicatePinCredentialException.class,
        () -> credentials.save(PinCredential.register(adult, "encoded-other")));
    assertEquals(winner.id(), credentials.findByOlderAdultId(adult).orElseThrow().id());
  }

  @Test
  void concurrentEnrollmentReturnsOneSuccessAndOneExpectedConflict() throws Exception {
    String adult = UUID.randomUUID().toString();
    var start = new CountDownLatch(1);
    try (var workers = Executors.newFixedThreadPool(2)) {
      var first =
          workers.submit(
              () -> {
                start.await();
                return commands.register(new RegisterPinCommand(adult, "1234"));
              });
      var second =
          workers.submit(
              () -> {
                start.await();
                return commands.register(new RegisterPinCommand(adult, "5678"));
              });
      start.countDown();
      var results = List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
      assertEquals(1, results.stream().filter(Result::isSuccess).count());
      var failure =
          (Result.Failure<?, ?>)
              results.stream().filter(Result::isFailure).findFirst().orElseThrow();
      assertEquals("PIN_ALREADY_REGISTERED", ((ApplicationError) failure.error()).code());
      assertTrue(credentials.findByOlderAdultId(adult).isPresent());
    }
  }
}
