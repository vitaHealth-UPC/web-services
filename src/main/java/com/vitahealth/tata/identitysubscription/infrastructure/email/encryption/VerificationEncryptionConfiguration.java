package com.vitahealth.tata.identitysubscription.infrastructure.email.encryption;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationPayloadCipher;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configures versioned encryption keys exclusively from external properties. */
@Configuration
@EnableConfigurationProperties(VerificationEncryptionConfiguration.Keys.class)
public class VerificationEncryptionConfiguration {
  /** External key ring; retains old versions until their pending requests are processed. */
  @ConfigurationProperties("tata.email.encryption")
  public record Keys(String activeKeyId, Map<String, String> keys) {}

  /** Fails startup if durable verification payload encryption has no usable key. */
  @Bean
  public VerificationPayloadCipher verificationPayloadCipher(Keys keys) {
    return new AesGcmVerificationPayloadCipher(keys.activeKeyId(), keys.keys());
  }
}
