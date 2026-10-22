package com.vitahealth.tata.intakeexecution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.JsonPath;
import com.vitahealth.tata.accessibilitypreferences.application.commandservices.UpdateVoiceConfirmationCommandService;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateVoiceConfirmationCommand;
import com.vitahealth.tata.carelink.CareLinkHttpFixture;
import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
import com.vitahealth.tata.carelink.domain.model.commands.WithdrawCareLinkConsentCommand;
import com.vitahealth.tata.identitysubscription.application.commandservices.SessionCommandService;
import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.VoiceRecognitionPort;
import com.vitahealth.tata.intakeexecution.application.result.ValidatedVoiceAudio;
import com.vitahealth.tata.intakeexecution.application.result.VoiceRecognitionResult;
import com.vitahealth.tata.intakeexecution.application.validation.VoiceAudioRequirements;
import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.VoiceConfirmationLanguage;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.intakeexecution.support.WavAudioFixture;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.WebApplicationContext;

/** Uses the real servlet and authenticated multipart HTTP to verify audio limits and validation. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class IntakeVoiceUploadHttpIntegrationTest {
  @Value("${local.server.port}")
  int port;

  @Autowired WebApplicationContext context;
  @Autowired IntakeRepository repository;
  @Autowired SessionCommandService sessions;
  @Autowired UpdateVoiceConfirmationCommandService preferences;

  @MockitoBean(enforceOverride = true)
  VoiceRecognitionPort recognition;

  HttpClient client;
  CareLinkHttpFixture fixture;
  String token;
  String intakeId;

  @BeforeEach
  void prepareAuthorizedPendingIntake() {
    fixture = CareLinkHttpFixture.approved(context);
    token = fixture.adultToken();
    preferences.handle(new UpdateVoiceConfirmationCommand(fixture.olderAdultId(), true));
    intakeId =
        repository
            .saveAll(
                List.of(
                    Intake.createScheduled(
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        fixture.olderAdultId(),
                        new MedicationSnapshot("Losartán", "1 comprimido", ""),
                        Instant.now().plusSeconds(60),
                        Instant.now())))
            .getFirst()
            .id();
    client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  }

  @AfterEach
  void closeHttpClient() {
    client.close();
  }

  @Test
  void rejectsConsentWithdrawnDuringRecognitionForTheCaregiver() throws Exception {
    token = fixture.caregiverToken();
    when(recognition.recognize(
            any(ValidatedVoiceAudio.class), eq(new VoiceConfirmationLanguage("es-419"))))
        .thenAnswer(
            invocation -> {
              var withdrawal =
                  context
                      .getBean(CareLinkCommandService.class)
                      .withdrawConsent(
                          new WithdrawCareLinkConsentCommand(
                              fixture.linkId(), fixture.olderAdultId()));
              assertTrue(withdrawal.isSuccess());
              return VoiceRecognitionResult.recognized("ya tome Losartán", 0.99);
            });

    var response = upload(WavAudioFixture.shortRecording());

    assertEquals(403, response.statusCode());
    assertEquals(IntakeStatus.PENDING, repository.findById(intakeId).orElseThrow().status());
  }

  @Test
  void rejectsSessionRevokedDuringRecognitionWithoutConfirmingTheIntake() throws Exception {
    when(recognition.recognize(
            any(ValidatedVoiceAudio.class), eq(new VoiceConfirmationLanguage("es-419"))))
        .thenAnswer(
            invocation -> {
              sessions.revoke(token);
              return VoiceRecognitionResult.recognized("ya tome Losartán", 0.99);
            });

    var response = upload(WavAudioFixture.shortRecording());

    assertEquals(403, response.statusCode());
    assertEquals(IntakeStatus.PENDING, repository.findById(intakeId).orElseThrow().status());
  }

  @Test
  void permitsExactlyFiveMiBThroughTheRealServlet() throws Exception {
    when(recognition.recognize(
            any(ValidatedVoiceAudio.class), eq(new VoiceConfirmationLanguage("es-419"))))
        .thenReturn(VoiceRecognitionResult.unrecognized(null, 0));
    var response = upload(WavAudioFixture.mono(48000, (VoiceAudioRequirements.MAX_BYTES - 44) / 2));
    assertEquals(200, response.statusCode());
    assertEquals("NOT_RECOGNIZED", JsonPath.read(response.body(), "$.status"));
    verify(recognition)
        .recognize(any(ValidatedVoiceAudio.class), eq(new VoiceConfirmationLanguage("es-419")));
    assertEquals(IntakeStatus.PENDING, repository.findById(intakeId).orElseThrow().status());
  }

  @Test
  void rejectsMalformedRecordingBeforeProviderDelivery() throws Exception {
    var response = upload(new byte[] {1, 2, 3});
    assertEquals(400, response.statusCode());
    assertEquals("VALIDATION_ERROR", JsonPath.read(response.body(), "$.title"));
    verifyNoInteractions(recognition);
    assertEquals(IntakeStatus.PENDING, repository.findById(intakeId).orElseThrow().status());
  }

  @Test
  void servletRejectsAnOversizedFileBeforeProviderDelivery() throws Exception {
    var response =
        upload(WavAudioFixture.mono(48000, (VoiceAudioRequirements.MAX_BYTES - 44) / 2 + 1));
    assertEquals(413, response.statusCode());
    verifyNoInteractions(recognition);
    assertEquals(IntakeStatus.PENDING, repository.findById(intakeId).orElseThrow().status());
  }

  private HttpResponse<String> upload(byte[] audio) throws Exception {
    var boundary = "tata-test-" + UUID.randomUUID();
    var prefix =
        ("--"
                + boundary
                + "\r\nContent-Disposition: form-data; name=\"audio\"; filename=\"voice.wav\"\r\n"
                + "Content-Type: audio/wav\r\n\r\n")
            .getBytes(StandardCharsets.US_ASCII);
    var suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII);
    var body =
        ByteBuffer.allocate(prefix.length + audio.length + suffix.length)
            .put(prefix)
            .put(audio)
            .put(suffix)
            .array();
    var request =
        HttpRequest.newBuilder(
                URI.create(
                    "http://127.0.0.1:"
                        + port
                        + "/api/v1/intakes/"
                        + intakeId
                        + "/voice-confirmation?language=es-419"))
            .timeout(Duration.ofSeconds(20))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
