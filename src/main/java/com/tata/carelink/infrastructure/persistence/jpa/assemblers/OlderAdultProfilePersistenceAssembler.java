package com.tata.carelink.infrastructure.persistence.jpa.assemblers;

import com.tata.carelink.domain.model.aggregates.OlderAdultProfile;
import com.tata.carelink.domain.model.valueobjects.EmergencyContact;
import com.tata.carelink.domain.model.valueobjects.OlderAdultBasicData;
import com.tata.carelink.infrastructure.persistence.jpa.entities.OlderAdultProfilePersistenceEntity;

/** Converts OlderAdultProfile state between domain and JPA representations without database access. */
public final class OlderAdultProfilePersistenceAssembler {
    private OlderAdultProfilePersistenceAssembler() {}

    /**
     * Restores the stored identity and state without executing a business transition.
     * @param entity stored JPA representation
     * @return reconstructed domain object
     */
    public static OlderAdultProfile toDomain(OlderAdultProfilePersistenceEntity entity) {
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

    /**
     * Builds the persistence representation while preserving the domain identity.
     * @return JPA state ready for the repository adapter
     */
    public static OlderAdultProfilePersistenceEntity toEntity(OlderAdultProfile profile) {
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
