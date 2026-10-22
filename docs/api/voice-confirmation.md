# Voice confirmation

`POST /api/v1/intakes/{intakeId}/voice-confirmation` accepts the multipart `audio` field and a `language` query parameter. Supported values are `es-419` (default), `es-PE`, `es-ES` and `en-US`. Unknown languages return request validation failure before transcription. The recording contract is PCM16 mono WAV, 8000-48000 Hz, up to 60 seconds and 5 MiB; the container is inspected before external delivery.

Google Cloud Speech-to-Text v1 receives JSON containing `config` and Base64 `audio.content`. The configuration uses the inspected sample rate, `LINEAR16` and one ranked alternative. The product locale `es-419` maps explicitly to Google v1 `es-PE`; other supported regional tags are preserved. Sequential top-ranked segments are retained in order, with the least available confidence; missing confidence is zero. A secondary affirmative alternative never replaces a principal negative phrase.

Enable the provider with `TATA_GOOGLE_SPEECH_ENABLED=true` and configure Application Default Credentials on the server. `GOOGLE_APPLICATION_CREDENTIALS` can identify an externally mounted credential file; hosted Google environments can use their attached identity. The identity requires Speech-to-Text access and the `cloud-platform` OAuth scope. Credentials are loaded lazily, refreshed by Google Auth, checked for expiry and never returned by this API. ADC quota-project attribution is preserved through `x-goog-user-project`. HTTP connection and response waits are bounded at five and thirty seconds respectively.

Recognition executes outside the handler's database transaction. The final confirmation rechecks the current voice preference under the intake lock. Definitive retries retain their original timestamp and channel. Provider failures leave the intake unchanged and return `PROVIDER_UNAVAILABLE`; unavailable configuration is never represented as successful recognition. Audio and provider transcripts are neither persisted nor explicitly logged. Google audio request DTOs and recognition hypotheses use redacted diagnostic rendering.

Confirmation requires a supported explicit completed-action phrase and confidence of at least 0.70. Questions, negation and explicit uncertainty return `NOT_VALIDATED`, even with high recognition confidence. Generic references such as “mi medicamento” apply to the selected intake; speech does not select another medication or intake.

Provider protocol reference: https://docs.cloud.google.com/speech-to-text/docs/reference/rest/v1/speech/recognize

Supported provider languages: https://docs.cloud.google.com/speech-to-text/docs/v1/speech-to-text-supported-languages

Application Default Credentials: https://cloud.google.com/docs/authentication/application-default-credentials
