package com.vitahealth.tata.intakeexecution.infrastructure.speech.google;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.vitahealth.tata.intakeexecution.application.result.RecognitionStatus;
import com.vitahealth.tata.intakeexecution.application.result.ValidatedVoiceAudio;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.VoiceConfirmationLanguage;
import com.vitahealth.tata.intakeexecution.infrastructure.audio.WavVoiceAudioValidator;
import com.vitahealth.tata.intakeexecution.infrastructure.external.adapters.VoiceRecognitionAdapter;
import com.vitahealth.tata.intakeexecution.infrastructure.speech.google.auth.GoogleSpeechAuthorization;
import com.vitahealth.tata.intakeexecution.support.WavAudioFixture;
import java.net.SocketTimeoutException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Exercises the real Google protocol against simulated HTTP, without credentials or cloud traffic.
 */
class GoogleSpeechToTextProviderClientTest {
  private static final String ENDPOINT = "https://speech.googleapis.com/v1/speech:recognize";
  private final ValidatedVoiceAudio audio =
      new WavVoiceAudioValidator().validate(WavAudioFixture.shortRecording(), "audio/wav");
  private final VoiceConfirmationLanguage spanish = new VoiceConfirmationLanguage("es-419");

  @Test
  void sendsActualGoogleJsonWithOAuthQuotaProjectAndInspectedSampleRate() {
    var builder = RestClient.builder().baseUrl("https://speech.googleapis.com");
    var server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo(ENDPOINT))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer test-token"))
        .andExpect(header("x-goog-user-project", "test-quota-project"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.config.encoding").value("LINEAR16"))
        .andExpect(jsonPath("$.config.sampleRateHertz").value(16000))
        .andExpect(jsonPath("$.config.languageCode").value("es-PE"))
        .andExpect(jsonPath("$.config.maxAlternatives").value(1))
        .andExpect(
            jsonPath("$.audio.content").value(Base64.getEncoder().encodeToString(audio.audio())))
        .andRespond(
            withSuccess(
                "{\"results\":[{\"alternatives\":[{\"transcript\":\"Ya tomé"
                    + " Losartán\",\"confidence\":0.94,\"words\":[]}]}],\"totalBilledTime\":\"1s\"}",
                MediaType.APPLICATION_JSON));
    var client =
        new GoogleSpeechToTextProviderClient(
            builder.build(),
            () -> new GoogleSpeechAuthorization("test-token", "test-quota-project"),
            true);
    var result = client.transcribe(audio, spanish);
    assertEquals("Ya tomé Losartán", result.transcript());
    assertEquals(0.94, result.confidence());
    server.verify();
  }

  @Test
  void disabledProviderDoesNotLoadCredentialsOrSendAudio() {
    var builder = RestClient.builder().baseUrl("https://speech.googleapis.com");
    var server = MockRestServiceServer.bindTo(builder).build();
    var calls = new AtomicInteger();
    var provider =
        new GoogleSpeechToTextProviderClient(
            builder.build(),
            () -> {
              calls.incrementAndGet();
              return new GoogleSpeechAuthorization("test-token", null);
            },
            false);
    var result = new VoiceRecognitionAdapter(provider).recognize(audio, spanish);
    assertEquals(RecognitionStatus.UNAVAILABLE, result.status());
    assertEquals(0, calls.get());
    server.verify();
  }

  @Test
  void credentialFailureDoesNotSendAnAnonymousRequestOrExposeProviderText() {
    var builder = RestClient.builder().baseUrl("https://speech.googleapis.com");
    var server = MockRestServiceServer.bindTo(builder).build();
    var provider =
        new GoogleSpeechToTextProviderClient(
            builder.build(),
            () -> {
              throw new IllegalStateException("sensitive credential diagnostic");
            },
            true);
    var result = new VoiceRecognitionAdapter(provider).recognize(audio, spanish);
    assertEquals(RecognitionStatus.UNAVAILABLE, result.status());
    assertEquals("speech-to-text provider failure", result.detail());
    server.verify();
  }

  @Test
  void providerAuthorizationQuotaAndServerFailuresStayUnavailable() {
    for (var status :
        List.of(
            HttpStatus.UNAUTHORIZED,
            HttpStatus.FORBIDDEN,
            HttpStatus.TOO_MANY_REQUESTS,
            HttpStatus.INTERNAL_SERVER_ERROR)) {
      var builder = RestClient.builder().baseUrl("https://speech.googleapis.com");
      var server = MockRestServiceServer.bindTo(builder).build();
      server
          .expect(requestTo(ENDPOINT))
          .andRespond(withStatus(status).body("sensitive provider response"));
      var result = adapter(builder).recognize(audio, spanish);
      assertEquals(RecognitionStatus.UNAVAILABLE, result.status());
      assertEquals("speech-to-text provider failure", result.detail());
      assertNull(result.transcript());
      server.verify();
    }
  }

  @Test
  void rejectsMalformedEmptyAndOutOfRangeResponses() {
    for (var response :
        List.of(
            "",
            "not-json",
            "{\"results\":[{\"alternatives\":[{\"transcript\":\"Ya tomé\",\"confidence\":2}]}]}")) {
      var builder = RestClient.builder().baseUrl("https://speech.googleapis.com");
      var server = MockRestServiceServer.bindTo(builder).build();
      server
          .expect(requestTo(ENDPOINT))
          .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
      assertEquals(
          RecognitionStatus.UNAVAILABLE, adapter(builder).recognize(audio, spanish).status());
      server.verify();
    }
  }

  @Test
  void emptySpeechAndMissingConfidenceAreNotPromotedToCertainty() {
    var builder = RestClient.builder().baseUrl("https://speech.googleapis.com");
    var server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo(ENDPOINT)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    server
        .expect(requestTo(ENDPOINT))
        .andRespond(
            withSuccess(
                "{\"results\":[{\"alternatives\":[{\"transcript\":\"Ya tomé Losartán\"}]}]}",
                MediaType.APPLICATION_JSON));
    var adapter = adapter(builder);
    assertEquals(RecognitionStatus.UNRECOGNIZED, adapter.recognize(audio, spanish).status());
    var uncertain = adapter.recognize(audio, spanish);
    assertEquals(0.0, uncertain.confidence());
    server.verify();
  }

  @Test
  void transportTimeoutStaysAControlledUnavailableObservation() {
    var builder = RestClient.builder().baseUrl("https://speech.googleapis.com");
    var server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo(ENDPOINT))
        .andRespond(withException(new SocketTimeoutException("sensitive timeout detail")));
    assertEquals(
        RecognitionStatus.UNAVAILABLE, adapter(builder).recognize(audio, spanish).status());
    server.verify();
  }

  @Test
  void requestDiagnosticsNeverRenderRawAudio() {
    var request = GoogleRecognitionRequestAssembler.toRequest(audio, spanish);
    var raw = Base64.getEncoder().encodeToString(audio.audio());
    assertTrue(!request.toString().contains(raw));
    assertTrue(!request.audio().toString().contains(raw));
  }

  private static VoiceRecognitionAdapter adapter(RestClient.Builder builder) {
    return new VoiceRecognitionAdapter(
        new GoogleSpeechToTextProviderClient(
            builder.build(), () -> new GoogleSpeechAuthorization("test-token", null), true));
  }
}
