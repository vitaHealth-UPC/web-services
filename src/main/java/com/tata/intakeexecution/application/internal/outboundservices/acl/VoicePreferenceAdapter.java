package com.tata.intakeexecution.application.internal.outboundservices.acl;

import com.tata.accessibilitypreferences.interfaces.acl.PreferencesContextFacade;
import com.tata.intakeexecution.application.internal.outboundservices.IVoicePreferencePort;
import org.springframework.stereotype.Component;

/** Reads the voice confirmation preference through the public contract of Accessibility & Preferences. */
@Component
public class VoicePreferenceAdapter implements IVoicePreferencePort {
    private final PreferencesContextFacade preferences;

    public VoicePreferenceAdapter(PreferencesContextFacade preferences) {
        this.preferences = preferences;
    }

    @Override
    public boolean isVoiceConfirmationEnabled(String olderAdultId) {
        return preferences.isVoiceConfirmationEnabled(olderAdultId);
    }
}
