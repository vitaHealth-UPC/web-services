package com.tata.intakeexecution.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.tata.intakeexecution.application.internal.outboundservices.IVoiceRecognitionPort.RecognitionStatus;
import com.tata.intakeexecution.infrastructure.external.adapters.VoiceRecognitionAdapter;
import com.tata.intakeexecution.infrastructure.external.dto.SpeechToTextProviderResponse;
import org.junit.jupiter.api.Test;

class VoiceRecognitionAdapterTest {

    @Test
    void mapsProviderTranscriptAndConfidence() {
        var adapter = new VoiceRecognitionAdapter(
                (audio, contentType, language) ->
                        new SpeechToTextProviderResponse(
                                "Confirmo que tomé Losartán.",
                                0.93
                        )
        );

        var result = adapter.recognize(
                new byte[]{1, 2, 3},
                "audio/wav",
                "es-419"
        );

        assertThat(result.status()).isEqualTo(RecognitionStatus.RECOGNIZED);
        assertThat(result.transcript()).isEqualTo("Confirmo que tomé Losartán.");
        assertThat(result.confidence()).isEqualTo(0.93);
    }

    @Test
    void blankTranscriptIsUnrecognized() {
        var adapter = new VoiceRecognitionAdapter(
                (audio, contentType, language) ->
                        new SpeechToTextProviderResponse(" ", 0.31)
        );

        var result = adapter.recognize(
                new byte[]{1},
                "audio/wav",
                "es-419"
        );

        assertThat(result.status()).isEqualTo(RecognitionStatus.UNRECOGNIZED);
        assertThat(result.confidence()).isEqualTo(0.31);
    }

    @Test
    void providerFailureBecomesControlledUnavailableOutcome() {
        var adapter = new VoiceRecognitionAdapter((audio, contentType, language) -> {
            throw new IllegalStateException("provider timeout");
        });

        var result = adapter.recognize(
                new byte[]{1},
                "audio/wav",
                "es-419"
        );

        assertThat(result.status()).isEqualTo(RecognitionStatus.UNAVAILABLE);
        assertThat(result.detail()).isEqualTo("provider timeout");
    }
}
