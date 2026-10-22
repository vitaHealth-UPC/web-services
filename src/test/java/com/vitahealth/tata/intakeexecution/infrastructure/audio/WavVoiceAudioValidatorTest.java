package com.vitahealth.tata.intakeexecution.infrastructure.audio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.intakeexecution.application.validation.VoiceAudioRequirements;
import com.vitahealth.tata.intakeexecution.support.WavAudioFixture;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Exercises untrusted RIFF lengths, unsupported codecs and exact audio input boundaries. */
class WavVoiceAudioValidatorTest {
  private final WavVoiceAudioValidator validator = new WavVoiceAudioValidator();

  @Test
  void readsRealMetadataAndKeepsDefensiveCopies() {
    var source = WavAudioFixture.mono(16000, 16000);
    var original = source.clone();
    var result = validator.validate(source, "AuDio/X-WaV; charset=binary");
    assertEquals(16000, result.sampleRateHertz());
    assertEquals(16000, result.frameCount());
    assertEquals("audio/wav", result.contentType());
    source[44] = 42;
    var returned = result.audio();
    returned[44] = 24;
    assertArrayEquals(original, result.audio());
  }

  @Test
  void permitsFmtAndDataInEitherOrderAndSkipsPaddedUnknownChunks() {
    var source = WavAudioFixture.shortRecording();
    var format = Arrays.copyOfRange(source, 12, 36);
    var data = Arrays.copyOfRange(source, 36, source.length);
    var junk = new byte[] {'J', 'U', 'N', 'K', 1, 0, 0, 0, 42, 0};
    assertEquals(160, validator.validate(riff(junk, data, format), "audio/wave").frameCount());
  }

  @Test
  void supportsPcmFmtExtensionOnlyWhenItsExtraSizeIsZero() {
    var source = WavAudioFixture.shortRecording();
    var format = Arrays.copyOf(Arrays.copyOfRange(source, 12, 36), 26);
    ByteBuffer.wrap(format).order(ByteOrder.LITTLE_ENDIAN).putInt(4, 18);
    var data = Arrays.copyOfRange(source, 36, source.length);
    assertEquals(16000, validator.validate(riff(format, data), "audio/wav").sampleRateHertz());
    format[24] = 1;
    assertThrows(
        IllegalArgumentException.class, () -> validator.validate(riff(format, data), "audio/wav"));
  }

  @Test
  void acceptsExactlySixtySecondsAndRejectsOneExtraFrame() {
    assertEquals(
        480000, validator.validate(WavAudioFixture.mono(8000, 480000), "audio/wav").frameCount());
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validate(WavAudioFixture.mono(8000, 480001), "audio/wav"));
  }

  @Test
  void enforcesTheExactFiveMiBContainerLimit() {
    var frames = (VoiceAudioRequirements.MAX_BYTES - 44) / 2;
    assertEquals(
        VoiceAudioRequirements.MAX_BYTES,
        validator.validate(WavAudioFixture.mono(48000, frames), "audio/wav").audio().length);
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validate(WavAudioFixture.mono(48000, frames + 1), "audio/wav"));
  }

  @Test
  void rejectsMimeSpoofingAndContainerOrRiffLengthCorruption() {
    var valid = WavAudioFixture.shortRecording();
    for (var mime : List.of("audio/mpeg", "application/octet-stream", "text/plain", ""))
      assertThrows(IllegalArgumentException.class, () -> validator.validate(valid, mime));
    for (int offset : new int[] {0, 8, 4}) {
      var broken = valid.clone();
      broken[offset] ^= 1;
      assertThrows(IllegalArgumentException.class, () -> validator.validate(broken, "audio/wav"));
    }
    assertThrows(
        IllegalArgumentException.class, () -> validator.validate(new byte[11], "audio/wav"));
  }

  @Test
  void rejectsUnsupportedFormatChannelsBitsAlignmentAndByteRate() {
    for (int offset : new int[] {20, 22, 28, 32, 34}) {
      var broken = WavAudioFixture.shortRecording();
      broken[offset] ^= 2;
      assertThrows(IllegalArgumentException.class, () -> validator.validate(broken, "audio/wav"));
    }
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validate(WavAudioFixture.mono(7999, 10), "audio/wav"));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validate(WavAudioFixture.mono(48001, 10), "audio/wav"));
  }

  @Test
  void rejectsUnsignedChunkOverflowTruncationAndMissingOddPadding() {
    var source = WavAudioFixture.shortRecording();
    var oversizedChunk = source.clone();
    ByteBuffer.wrap(oversizedChunk).order(ByteOrder.LITTLE_ENDIAN).putInt(40, -1);
    assertThrows(
        IllegalArgumentException.class, () -> validator.validate(oversizedChunk, "audio/wav"));
    var truncated = Arrays.copyOf(source, source.length - 2);
    ByteBuffer.wrap(truncated).order(ByteOrder.LITTLE_ENDIAN).putInt(4, truncated.length - 8);
    assertThrows(IllegalArgumentException.class, () -> validator.validate(truncated, "audio/wav"));
    var unpadded =
        riff(
            Arrays.copyOfRange(source, 12, source.length),
            new byte[] {'J', 'U', 'N', 'K', 1, 0, 0, 0, 42});
    assertThrows(IllegalArgumentException.class, () -> validator.validate(unpadded, "audio/wav"));
  }

  @Test
  void rejectsDuplicateOrMissingFormatAndDataChunks() {
    var source = WavAudioFixture.shortRecording();
    var format = Arrays.copyOfRange(source, 12, 36);
    var data = Arrays.copyOfRange(source, 36, source.length);
    for (var broken :
        List.of(riff(format), riff(data), riff(format, format, data), riff(format, data, data)))
      assertThrows(IllegalArgumentException.class, () -> validator.validate(broken, "audio/wav"));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validate(WavAudioFixture.mono(16000, 0), "audio/wav"));
  }

  private static byte[] riff(byte[]... chunks) {
    int length = 12;
    for (var chunk : chunks) length += chunk.length;
    var result = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
    result.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(length - 8);
    result.put("WAVE".getBytes(StandardCharsets.US_ASCII));
    for (var chunk : chunks) result.put(chunk);
    return result.array();
  }
}
