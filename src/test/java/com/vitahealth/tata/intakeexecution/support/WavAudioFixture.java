package com.vitahealth.tata.intakeexecution.support;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** Produces silent PCM16 mono recordings for codec and orchestrator contract tests. */
public final class WavAudioFixture {
  private WavAudioFixture() {}

  /** Produces one short 16kHz recording with a standard 44-byte WAV header. */
  public static byte[] shortRecording() {
    return mono(16000, 160);
  }

  /** Produces exactly the requested number of mono PCM16 sample frames. */
  public static byte[] mono(int sampleRate, int frames) {
    var buffer = ByteBuffer.allocate(44 + frames * 2).order(ByteOrder.LITTLE_ENDIAN);
    buffer.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(buffer.capacity() - 8);
    buffer.put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16);
    buffer.putShort((short) 1).putShort((short) 1).putInt(sampleRate).putInt(sampleRate * 2);
    buffer.putShort((short) 2).putShort((short) 16);
    buffer.put("data".getBytes(StandardCharsets.US_ASCII)).putInt(frames * 2);
    return buffer.array();
  }
}
