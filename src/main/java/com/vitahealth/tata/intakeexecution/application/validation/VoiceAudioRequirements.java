package com.vitahealth.tata.intakeexecution.application.validation;

/** Input limits for synchronous PCM voice confirmation, shared by HTTP and codec validation. */
public final class VoiceAudioRequirements {
  public static final int MAX_BYTES = 5 * 1024 * 1024;
  public static final int MAX_SECONDS = 60;
  public static final int MIN_SAMPLE_RATE = 8000;
  public static final int MAX_SAMPLE_RATE = 48000;

  private VoiceAudioRequirements() {}
}
