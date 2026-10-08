package com.tata.treatmentmanagement.application.internal.queryservices;

import com.tata.treatmentmanagement.application.internal.TreatmentMapper;
import com.tata.treatmentmanagement.application.models.MedicationCatalogItem;
import com.tata.treatmentmanagement.application.queryservices.MedicationCatalogQueryService;
import com.tata.treatmentmanagement.domain.model.queries.GetMyMedicationCatalogQuery;
import com.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MedicationCatalogQueryServiceImpl implements MedicationCatalogQueryService {
    private final MedicationRepository medications;
    private final TreatmentRepository treatments;

    public MedicationCatalogQueryServiceImpl(MedicationRepository medications, TreatmentRepository treatments) {
        this.medications = medications;
        this.treatments = treatments;
    }

    @Override
    public List<MedicationCatalogItem> handle(GetMyMedicationCatalogQuery query) {
        var ownedTreatments = treatments.findByOlderAdultId(query.olderAdultId());
        return medications.findByOlderAdultId(query.olderAdultId()).stream()
                .map(medication -> new MedicationCatalogItem(TreatmentMapper.toResult(medication),
                        ownedTreatments.stream()
                                .filter(treatment -> treatment.regimen() != null
                                        && medication.id().equals(treatment.regimen().medicationId()))
                                .map(TreatmentMapper::toResult).toList()))
                .toList();
    }
}
