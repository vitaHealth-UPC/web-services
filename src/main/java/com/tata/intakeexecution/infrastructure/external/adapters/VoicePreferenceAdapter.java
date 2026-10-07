package com.tata.intakeexecution.infrastructure.external.adapters;

import com.tata.accessibilitypreferences.application.queryservices.UserPreferencesQueryService;
import com.tata.intakeexecution.application.internal.outboundservices.IVoicePreferencePort;
import org.springframework.stereotype.Component;

/** Reads the voice confirmation preference through the public contract of Accessibility & Preferences. */
@Component
public class VoicePreferenceAdapter implements IVoicePreferencePort {
    private final UserPreferencesQueryService preferences;

    public VoicePreferenceAdapter(UserPreferencesQueryService preferences) {
        this.preferences = preferences;
    }

    @Override
    public boolean isVoiceConfirmationEnabled(String olderAdultId) {
        return preferences.isVoiceConfirmationEnabled(olderAdultId);
    }
}
