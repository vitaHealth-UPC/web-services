package com.vitahealth.tata.intakeexecution.infrastructure.speech.google;

import com.vitahealth.tata.intakeexecution.application.result.ValidatedVoiceAudio;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.VoiceConfirmationLanguage;
import com.vitahealth.tata.intakeexecution.infrastructure.external.SpeechToTextProviderClient;
import com.vitahealth.tata.intakeexecution.infrastructure.external.dto.SpeechToTextProviderResponse;
import com.vitahealth.tata.intakeexecution.infrastructure.speech.google.auth.GoogleSpeechAuthorizationProvider;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Implements Google's actual synchronous JSON/Base64 protocol using renewable OAuth credentials.
 */
public class GoogleSpeechToTextProviderClient implements SpeechToTextProviderClient {
  private final RestClient client;
  private final GoogleSpeechAuthorizationProvider authorization;
  private final boolean enabled;

  /** Injects bounded HTTP, provider authorization and explicit deployment enablement. */
  public GoogleSpeechToTextProviderClient(
      RestClient client, GoogleSpeechAuthorizationProvider authorization, boolean enabled) {
    this.client = Objects.requireNonNull(client, "client");
    this.authorization = Objects.requireNonNull(authorization, "authorization");
    this.enabled = enabled;
  }

  /**
   * Sends validated recording metadata; no provider call is issued while the integration is
   * disabled.
   */
  @Override
  public SpeechToTextProviderResponse transcribe(
      ValidatedVoiceAudio audio, VoiceConfirmationLanguage language) {
    if (!enabled) throw new IllegalStateException("Google Speech is not enabled");
    var identity = authorization.authorization();
    var response =
        client
            .post()
            .uri("/v1/speech:recognize")
            .contentType(MediaType.APPLICATION_JSON)
            .headers(
                headers -> {
                  headers.setBearerAuth(identity.accessToken());
                  if (identity.quotaProjectId() != null && !identity.quotaProjectId().isBlank())
                    headers.set("x-goog-user-project", identity.quotaProjectId());
                })
            .body(GoogleRecognitionRequestAssembler.toRequest(audio, language))
            .retrieve()
            .body(GoogleRecognitionResponse.class);
    if (response == null)
      throw new IllegalStateException("Google Speech returned an empty response");
    return GoogleRecognitionResponseAssembler.toResponse(response);
  }
}
