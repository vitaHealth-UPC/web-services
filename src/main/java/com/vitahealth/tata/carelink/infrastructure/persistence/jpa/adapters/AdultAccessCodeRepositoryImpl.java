package com.vitahealth.tata.carelink.infrastructure.persistence.jpa.adapters;

import com.vitahealth.tata.carelink.domain.model.aggregates.AdultAccessCode;
import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.vitahealth.tata.carelink.domain.repositories.AdultAccessCodeRepository;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.entities.AdultAccessCodePersistenceEntity;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.repositories.AdultAccessCodeJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Maps adult sign-in codes between the domain and JPA. */
@Repository
@Transactional
public class AdultAccessCodeRepositoryImpl implements AdultAccessCodeRepository {
  private final AdultAccessCodeJpaRepository repository;

  /** Injects Spring Data storage. */
  public AdultAccessCodeRepositoryImpl(AdultAccessCodeJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public AdultAccessCode save(AdultAccessCode code) {
    return toDomain(
        repository.saveAndFlush(
            new AdultAccessCodePersistenceEntity(
                code.id(),
                code.careLinkId(),
                code.olderAdultId(),
                code.issuedByCaregiverId(),
                code.code().value(),
                code.code().expiresAt(),
                code.code().usedAt(),
                code.createdAt(),
                code.kind().name())));
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<AdultAccessCode> findByCodeForUpdate(String code) {
    return repository.findByCode(code).map(AdultAccessCodeRepositoryImpl::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean exists(String code) {
    return repository.existsByCode(code);
  }

  @Override
  public void deleteUnusedByCareLinkId(String careLinkId) {
    repository.deleteByCareLinkIdAndKindAndUsedAtIsNull(
        careLinkId, AdultAccessCode.Kind.ADULT_ACCESS.name());
  }

  @Override
  public void deleteUnused(String careLinkId, AdultAccessCode.Kind kind) {
    repository.deleteByCareLinkIdAndKindAndUsedAtIsNull(careLinkId, kind.name());
  }

  private static AdultAccessCode toDomain(AdultAccessCodePersistenceEntity entity) {
    return AdultAccessCode.rehydrate(
        entity.getId(),
        entity.getCareLinkId(),
        entity.getOlderAdultId(),
        entity.getIssuedBy(),
        LinkingCode.rehydrate(entity.getCode(), entity.getExpiresAt(), entity.getUsedAt()),
        entity.getCreatedAt(),
        entity.getKind() == null
            ? AdultAccessCode.Kind.ADULT_ACCESS
            : AdultAccessCode.Kind.valueOf(entity.getKind()));
  }
}
