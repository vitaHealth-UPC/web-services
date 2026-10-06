package com.tata.carelink.infrastructure.persistence.jpa.adapters;

import com.tata.carelink.domain.model.aggregates.OlderAdultProfile;
import com.tata.carelink.domain.model.valueobjects.EmergencyContact;
import com.tata.carelink.domain.model.valueobjects.OlderAdultBasicData;
import com.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import com.tata.carelink.infrastructure.persistence.jpa.entities.OlderAdultProfilePersistenceEntity;
import com.tata.carelink.infrastructure.persistence.jpa.repositories.OlderAdultProfileJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class OlderAdultProfileRepositoryImpl implements OlderAdultProfileRepository {
    private final OlderAdultProfileJpaRepository repository;

    public OlderAdultProfileRepositoryImpl(OlderAdultProfileJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public OlderAdultProfile save(OlderAdultProfile profile) {
        return toDomain(repository.save(toEntity(profile)));
    }

    @Override
    public Optional<OlderAdultProfile> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    private OlderAdultProfile toDomain(OlderAdultProfilePersistenceEntity entity) {
        EmergencyContact contact = null;
        if (entity.getEmergencyContactName() != null) {
            contact = new EmergencyContact(
                    entity.getEmergencyContactName(),
                    entity.getEmergencyContactRelationship(),
                    entity.getEmergencyContactPhone()
            );
        }
        return OlderAdultProfile.rehydrate(
                entity.getId(),
                entity.getRegisteredByCaregiverId(),
                new OlderAdultBasicData(entity.getFullName(), entity.getBirthDate()),
                contact,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private OlderAdultProfilePersistenceEntity toEntity(OlderAdultProfile profile) {
        var contact = profile.emergencyContact();
        return new OlderAdultProfilePersistenceEntity(
                profile.id(),
                profile.registeredByCaregiverId(),
                profile.basicData().fullName(),
                profile.basicData().birthDate(),
                contact == null ? null : contact.name(),
                contact == null ? null : contact.relationship(),
                contact == null ? null : contact.phone(),
                profile.createdAt(),
                profile.updatedAt()
        );
    }
}
