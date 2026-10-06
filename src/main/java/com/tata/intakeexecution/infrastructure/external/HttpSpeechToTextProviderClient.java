package com.tata.intakeexecution.infrastructure.external;

import com.tata.intakeexecution.infrastructure.external.dto.SpeechToTextProviderResponse;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpSpeechToTextProviderClient implements SpeechToTextProviderClient {

    private final RestClient restClient;
    private final String endpoint;
    private final String apiKey;

    public HttpSpeechToTextProviderClient(
            @Value("${tata.speech-to-text.endpoint:}") String endpoint,
            @Value("${tata.speech-to-text.api-key:}") String apiKey
    ) {
        this.restClient = RestClient.builder().build();
        this.endpoint = endpoint == null ? "" : endpoint.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    public SpeechToTextProviderResponse transcribe(
            byte[] audio,
            String contentType,
            String language
    ) {
        if (endpoint.isBlank()) {
            throw new IllegalStateException("speech-to-text provider is not configured");
        }

        var request = restClient.post()
                .uri(endpoint)
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.ACCEPT_LANGUAGE, language);

        if (!apiKey.isBlank()) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }

        return Objects.requireNonNull(
                request
                        .body(audio)
                        .retrieve()
                        .body(SpeechToTextProviderResponse.class),
                "speech-to-text provider returned an empty response"
        );
    }
}
