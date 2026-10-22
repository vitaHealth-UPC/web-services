package com.vitahealth.tata.intakeexecution.infrastructure.acl;

import com.vitahealth.tata.accessibilitypreferences.interfaces.acl.PreferencesContextFacade;
import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.VoicePreferencePort;
import org.springframework.stereotype.Component;

/**
 * Reads the voice confirmation preference through the public contract of Accessibility &
 * Preferences.
 */
@Component
public class VoicePreferenceAdapter implements VoicePreferencePort {
  private final PreferencesContextFacade preferences;

  /** Injects the public preferences boundary rather than its persistence implementation. */
  public VoicePreferenceAdapter(PreferencesContextFacade preferences) {
    this.preferences = preferences;
  }

  /** Reads the current voice setting and propagates technical failures without enabling access. */
  @Override
  public boolean isVoiceConfirmationEnabled(String olderAdultId) {
    return preferences.isVoiceConfirmationEnabled(olderAdultId);
  }
}
