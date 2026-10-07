package com.tata.intakeexecution.infrastructure.external;

import com.tata.intakeexecution.infrastructure.external.dto.SpeechToTextProviderResponse;

public interface SpeechToTextProviderClient {

    SpeechToTextProviderResponse transcribe(
            byte[] audio,
            String contentType,
            String language
    );
}
