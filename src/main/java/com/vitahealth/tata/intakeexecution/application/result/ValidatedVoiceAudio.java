package com.vitahealth.tata.intakeexecution.application.result;

import com.vitahealth.tata.intakeexecution.application.validation.VoiceAudioRequirements;

/** Bounded audio and metadata produced by the codec-validation boundary; never persisted. */
public record ValidatedVoiceAudio(byte[] audio, int sampleRateHertz, long frameCount) {
  /** Snapshots the bounded recording and validates its mono PCM sample metadata. */
  public ValidatedVoiceAudio {
    if (audio == null || audio.length == 0 || audio.length > VoiceAudioRequirements.MAX_BYTES)
      throw new IllegalArgumentException("audio must fit the voice input limit");
    if (sampleRateHertz < VoiceAudioRequirements.MIN_SAMPLE_RATE
        || sampleRateHertz > VoiceAudioRequirements.MAX_SAMPLE_RATE
        || frameCount <= 0
        || frameCount > (long) sampleRateHertz * VoiceAudioRequirements.MAX_SECONDS)
      throw new IllegalArgumentException("audio metadata exceeds the supported recording limits");
    audio = audio.clone();
  }

  /** Returns a defensive copy so downstream providers cannot mutate the validated observation. */
  @Override
  public byte[] audio() {
    return audio.clone();
  }

  /** Canonical MIME type after the WAV container has been inspected. */
  public String contentType() {
    return "audio/wav";
  }
}
