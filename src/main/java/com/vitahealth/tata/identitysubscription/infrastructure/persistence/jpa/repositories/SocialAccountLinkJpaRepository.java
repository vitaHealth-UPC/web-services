package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.SocialAccountLinkPersistenceEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountLinkJpaRepository extends JpaRepository<SocialAccountLinkPersistenceEntity, String> {
  Optional<SocialAccountLinkPersistenceEntity> findByIssuerAndSubject(String issuer, String subject);
  void deleteByAccountId(String accountId);
}
