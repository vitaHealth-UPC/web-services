package com.vitahealth.tata.intakeexecution.infrastructure.audio;

import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.VoiceAudioValidationPort;
import com.vitahealth.tata.intakeexecution.application.result.ValidatedVoiceAudio;
import com.vitahealth.tata.intakeexecution.application.validation.VoiceAudioRequirements;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Reads bounded RIFF chunks and accepts only uncompressed signed 16-bit mono PCM WAV. */
@Component
public class WavVoiceAudioValidator implements VoiceAudioValidationPort {
  private static final Set<String> MIME_TYPES = Set.of("audio/wav", "audio/x-wav", "audio/wave");

  /**
   * Validates container, format, chunk lengths, sample rate and duration without decoding speech.
   */
  @Override
  public ValidatedVoiceAudio validate(byte[] audio, String contentType) {
    if (audio == null || audio.length < 12 || audio.length > VoiceAudioRequirements.MAX_BYTES)
      throw invalid();
    var mime =
        contentType == null ? "" : contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    if (!MIME_TYPES.contains(mime)
        || !tag(audio, 0, "RIFF")
        || !tag(audio, 8, "WAVE")
        || unsigned32(audio, 4) != audio.length - 8L) throw invalid();
    boolean formatFound = false;
    boolean dataFound = false;
    int sampleRate = 0;
    long dataBytes = 0;
    int offset = 12;
    while (offset < audio.length) {
      if (audio.length - offset < 8) throw invalid();
      var size = unsigned32(audio, offset + 4);
      int payload = offset + 8;
      long end = payload + size;
      long paddedEnd = end + (size & 1L);
      if (end > audio.length || paddedEnd > audio.length) throw invalid();
      if (tag(audio, offset, "fmt ")) {
        if (formatFound || (size != 16 && size != 18)) throw invalid();
        if (unsigned16(audio, payload) != 1
            || unsigned16(audio, payload + 2) != 1
            || unsigned16(audio, payload + 12) != 2
            || unsigned16(audio, payload + 14) != 16
            || (size == 18 && unsigned16(audio, payload + 16) != 0)) throw invalid();
        long rate = unsigned32(audio, payload + 4);
        if (rate < VoiceAudioRequirements.MIN_SAMPLE_RATE
            || rate > VoiceAudioRequirements.MAX_SAMPLE_RATE
            || unsigned32(audio, payload + 8) != rate * 2) throw invalid();
        sampleRate = (int) rate;
        formatFound = true;
      } else if (tag(audio, offset, "data")) {
        if (dataFound || size == 0 || (size & 1L) != 0) throw invalid();
        dataBytes = size;
        dataFound = true;
      }
      offset = (int) paddedEnd;
    }
    if (!formatFound
        || !dataFound
        || dataBytes > (long) sampleRate * 2 * VoiceAudioRequirements.MAX_SECONDS) throw invalid();
    return new ValidatedVoiceAudio(audio, sampleRate, dataBytes / 2);
  }

  private static boolean tag(byte[] value, int offset, String expected) {
    for (int i = 0; i < 4; i++) if ((value[offset + i] & 0xff) != expected.charAt(i)) return false;
    return true;
  }

  private static int unsigned16(byte[] value, int offset) {
    return (value[offset] & 0xff) | ((value[offset + 1] & 0xff) << 8);
  }

  private static long unsigned32(byte[] value, int offset) {
    return (value[offset] & 0xffL)
        | ((value[offset + 1] & 0xffL) << 8)
        | ((value[offset + 2] & 0xffL) << 16)
        | ((value[offset + 3] & 0xffL) << 24);
  }

  private static IllegalArgumentException invalid() {
    return new IllegalArgumentException("voice audio must be a bounded mono PCM16 WAV recording");
  }
}
