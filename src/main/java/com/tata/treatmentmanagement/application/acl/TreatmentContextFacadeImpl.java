package com.tata.treatmentmanagement.application.acl;

import com.tata.treatmentmanagement.application.models.MedicationResult;
import com.tata.treatmentmanagement.application.models.TreatmentResult;
import java.util.List;
import java.util.Optional;
import com.tata.treatmentmanagement.interfaces.acl.TreatmentContextFacade;
import com.tata.treatmentmanagement.application.queryservices.TreatmentQueryService;
import org.springframework.stereotype.Service;

@Service
public class TreatmentContextFacadeImpl implements TreatmentContextFacade {
    private final TreatmentQueryService queries;
    private final com.tata.treatmentmanagement.domain.repositories.TreatmentRepository treatments;
    public TreatmentContextFacadeImpl(TreatmentQueryService queries, com.tata.treatmentmanagement.domain.repositories.TreatmentRepository treatments) { this.queries = queries; this.treatments=treatments; }
    @Override public MedicationResult getMedication(String caregiverId, String medicationId) { return queries.getMedication(caregiverId, medicationId); }
    @Override public TreatmentResult getTreatment(String caregiverId, String treatmentId) { return queries.getTreatment(caregiverId, treatmentId); }
    @Override public List<MedicationResult> listMedications(String caregiverId, String olderAdultId) { return queries.listMedications(caregiverId, olderAdultId); }
    @Override public List<TreatmentResult> listTreatments(String caregiverId, String olderAdultId) { return queries.listTreatments(caregiverId, olderAdultId); }
    @Override public Optional<TreatmentResult> findTreatment(String id) { return treatments.findById(id).map(com.tata.treatmentmanagement.application.internal.TreatmentMapper::toResult); }
    @Override public Optional<MedicationResult> findMedication(String medicationId) { return queries.findMedication(medicationId); }
}
