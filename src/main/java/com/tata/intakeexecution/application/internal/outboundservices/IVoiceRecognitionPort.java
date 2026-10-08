package com.tata.intakeexecution.application.internal.outboundservices;

public interface IVoiceRecognitionPort {

    VoiceRecognitionResult recognize(byte[] audio, String contentType, String language);

    enum RecognitionStatus {
        RECOGNIZED,
        UNRECOGNIZED,
        UNAVAILABLE
    }

    record VoiceRecognitionResult(
            RecognitionStatus status,
            String transcript,
            double confidence,
            String detail
    ) {
        public static VoiceRecognitionResult recognized(String transcript, double confidence) {
            return new VoiceRecognitionResult(
                    RecognitionStatus.RECOGNIZED,
                    transcript,
                    normalizeConfidence(confidence),
                    null
            );
        }

        public static VoiceRecognitionResult unrecognized(String transcript, double confidence) {
            return new VoiceRecognitionResult(
                    RecognitionStatus.UNRECOGNIZED,
                    transcript,
                    normalizeConfidence(confidence),
                    null
            );
        }

        public static VoiceRecognitionResult unavailable(String detail) {
            return new VoiceRecognitionResult(
                    RecognitionStatus.UNAVAILABLE,
                    null,
                    0.0,
                    detail
            );
        }

        private static double normalizeConfidence(double value) {
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                return 0.0;
            }
            return Math.max(0.0, Math.min(1.0, value));
        }
    }
}
