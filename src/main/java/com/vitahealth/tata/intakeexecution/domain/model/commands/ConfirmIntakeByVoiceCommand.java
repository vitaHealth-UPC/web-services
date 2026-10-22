package com.vitahealth.tata.intakeexecution.domain.model.commands;

/** Untrusted recording input; the application validates its codec before provider delivery. */
public record ConfirmIntakeByVoiceCommand(
    String intakeId, byte[] audio, String contentType, String language) {
  /** Validates required fields and isolates the caller-owned byte array. */
  public ConfirmIntakeByVoiceCommand {
    if (intakeId == null || intakeId.isBlank()) {
      throw new IllegalArgumentException("intakeId is required");
    }
    if (audio == null || audio.length == 0) {
      throw new IllegalArgumentException("audio is required");
    }
    if (contentType == null || contentType.isBlank()) {
      throw new IllegalArgumentException("contentType is required");
    }
    if (language == null || language.isBlank()) {
      throw new IllegalArgumentException("language is required");
    }

    intakeId = intakeId.trim();
    audio = audio.clone();
    contentType = contentType.trim();
    language = language.trim();
  }

  /** Returns a defensive copy of the request recording. */
  @Override
  public byte[] audio() {
    return audio.clone();
  }
}
