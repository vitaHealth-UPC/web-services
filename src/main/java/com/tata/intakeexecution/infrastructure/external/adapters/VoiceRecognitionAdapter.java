package com.tata.intakeexecution.infrastructure.external.adapters;

import com.tata.intakeexecution.application.internal.outboundservices.IVoiceRecognitionPort;
import com.tata.intakeexecution.infrastructure.external.SpeechToTextProviderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class VoiceRecognitionAdapter implements IVoiceRecognitionPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceRecognitionAdapter.class);

    private final SpeechToTextProviderClient providerClient;

    public VoiceRecognitionAdapter(SpeechToTextProviderClient providerClient) {
        this.providerClient = providerClient;
    }

    @Override
    public VoiceRecognitionResult recognize(
            byte[] audio,
            String contentType,
            String language
    ) {
        try {
            var providerResult = providerClient.transcribe(audio, contentType, language);
            String transcript = providerResult.transcript();
            double confidence = providerResult.confidence() == null
                    ? 0.0
                    : providerResult.confidence();

            if (transcript == null || transcript.isBlank()) {
                return VoiceRecognitionResult.unrecognized(null, confidence);
            }

            return VoiceRecognitionResult.recognized(transcript.trim(), confidence);
        } catch (RuntimeException exception) {
            LOGGER.warn("Speech-to-text recognition failed: {}", exception.getMessage());
            return VoiceRecognitionResult.unavailable(
                    exception.getMessage() == null || exception.getMessage().isBlank()
                            ? "speech-to-text provider failure"
                            : exception.getMessage()
            );
        }
    }
}
