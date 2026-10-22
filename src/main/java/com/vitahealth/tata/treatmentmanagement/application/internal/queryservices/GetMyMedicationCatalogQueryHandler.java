package com.vitahealth.tata.treatmentmanagement.application.internal.queryservices;

import com.vitahealth.tata.treatmentmanagement.application.internal.TreatmentMapper;
import com.vitahealth.tata.treatmentmanagement.application.result.MedicationCatalogItem;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.GetMyMedicationCatalogQuery;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads the adult medication catalog using two repository reads and one treatment grouping. */
@Service
@Transactional(readOnly = true)
public class GetMyMedicationCatalogQueryHandler {
  private final MedicationRepository medications;
  private final TreatmentRepository treatments;

  /** Injects context-owned reads without loading medications once per treatment. */
  public GetMyMedicationCatalogQueryHandler(
      MedicationRepository medications, TreatmentRepository treatments) {
    this.medications = medications;
    this.treatments = treatments;
  }

  /** Preserves medication order and associated configured treatment history. */
  public List<MedicationCatalogItem> handle(GetMyMedicationCatalogQuery query) {
    var byMedication =
        treatments.findByOlderAdultId(query.olderAdultId()).stream()
            .filter(treatment -> treatment.regimen() != null)
            .collect(
                Collectors.groupingBy(
                    treatment -> treatment.regimen().medicationId(),
                    Collectors.mapping(TreatmentMapper::toResult, Collectors.toList())));
    return medications.findByOlderAdultId(query.olderAdultId()).stream()
        .map(
            medication ->
                new MedicationCatalogItem(
                    TreatmentMapper.toResult(medication),
                    byMedication.getOrDefault(medication.id(), List.of())))
        .toList();
  }
}
