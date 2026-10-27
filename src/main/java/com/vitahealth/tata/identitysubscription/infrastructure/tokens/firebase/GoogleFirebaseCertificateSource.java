package com.vitahealth.tata.identitysubscription.infrastructure.tokens.firebase;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

/** Fetches only Firebase's fixed HTTPS certificate endpoint, never a token-provided URL. */
public final class GoogleFirebaseCertificateSource implements Supplier<FirebaseSigningKeyCache.Snapshot> {
  private static final String URL = "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";
  private final RestClient client;
  private final Clock clock;
  public GoogleFirebaseCertificateSource(RestClient client, Clock clock) { this.client = client; this.clock = clock; }
  @Override public FirebaseSigningKeyCache.Snapshot get() {
    try {
      var response = client.get().uri(URL).retrieve().toEntity(new ParameterizedTypeReference<Map<String, String>>() {});
      var body = response.getBody();
      if (body == null || body.isEmpty() || body.size() > 20) throw new IllegalArgumentException();
      var factory = CertificateFactory.getInstance("X.509");
      var keys = new HashMap<String, PublicKey>();
      for (var entry : body.entrySet()) {
        if (entry.getKey().isBlank() || entry.getKey().length() > 256 || entry.getValue().length() > 16384) throw new IllegalArgumentException();
        var certificate = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(entry.getValue().getBytes(StandardCharsets.US_ASCII)));
        certificate.checkValidity(Date.from(clock.instant()));
        if (!"RSA".equals(certificate.getPublicKey().getAlgorithm())) throw new IllegalArgumentException();
        keys.put(entry.getKey(), certificate.getPublicKey());
      }
      var matcher = Pattern.compile("(?:^|,)\\s*max-age=(\\d+)").matcher(Objects.toString(response.getHeaders().getCacheControl(), ""));
      var lifetime = matcher.find() ? Duration.ofSeconds(Long.parseLong(matcher.group(1))) : Duration.ofMinutes(5);
      return new FirebaseSigningKeyCache.Snapshot(keys, lifetime);
    } catch (Exception failure) {
      throw new IllegalStateException("Firebase signing certificates unavailable");
    }
  }
}
