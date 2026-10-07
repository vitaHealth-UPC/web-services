package com.tata.adherenceanalytics.infrastructure.persistence.jpa.adapters;

import com.tata.adherenceanalytics.infrastructure.persistence.jpa.repositories.AdherenceSnapshotJpaRepository;
import com.tata.adherenceanalytics.domain.model.AdherencePeriodSnapshot;
import com.tata.adherenceanalytics.domain.repositories.AdherenceSnapshotRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import com.tata.adherenceanalytics.infrastructure.persistence.jpa.assemblers.AdherenceSnapshotPersistenceAssembler;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class AdherenceSnapshotRepositoryAdapter implements AdherenceSnapshotRepository {
    private final AdherenceSnapshotJpaRepository repository;
    private final TransactionTemplate writes;
    @PersistenceContext private EntityManager entities;
    public AdherenceSnapshotRepositoryAdapter(AdherenceSnapshotJpaRepository repository, PlatformTransactionManager manager) {
        this.repository = repository;
        writes = new TransactionTemplate(manager);
        writes.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    @Override public Optional<AdherencePeriodSnapshot> find(String id) { return repository.findById(id).map(AdherenceSnapshotPersistenceAssembler::toDomain); }
    @Override public AdherencePeriodSnapshot saveIfAbsent(AdherencePeriodSnapshot snapshot) {
        try {
            return writes.execute(status -> {
                var existing = repository.findById(snapshot.id());
                if (existing.isPresent()) return AdherenceSnapshotPersistenceAssembler.toDomain(existing.get());
                entities.persist(AdherenceSnapshotPersistenceAssembler.toEntity(snapshot));
                entities.flush();
                return snapshot;
            });
        } catch (DataIntegrityViolationException | org.hibernate.exception.ConstraintViolationException
                | jakarta.persistence.EntityExistsException exception) {
            return repository.findById(snapshot.id()).map(AdherenceSnapshotPersistenceAssembler::toDomain).orElseThrow(() -> exception);
        }
    }
}
