package com.tata.treatmentmanagement.application.internal.queryservices;

import com.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.tata.treatmentmanagement.application.internal.TreatmentMapper;
import com.tata.treatmentmanagement.application.models.MedicationResult;
import com.tata.treatmentmanagement.application.models.TreatmentResult;
import com.tata.treatmentmanagement.application.queryservices.TreatmentQueryService;
import com.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import com.tata.treatmentmanagement.domain.services.ICareLinkVerificationPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TreatmentQueryServiceImpl implements TreatmentQueryService {
    private final MedicationRepository medications;
    private final TreatmentRepository treatments;
    private final ICareLinkVerificationPort careLinkVerificationPort;

    public TreatmentQueryServiceImpl(
            MedicationRepository medications,
            TreatmentRepository treatments,
            ICareLinkVerificationPort careLinkVerificationPort
    ) {
        this.medications = medications;
        this.treatments = treatments;
        this.careLinkVerificationPort = careLinkVerificationPort;
    }

    @Override
    public MedicationResult getMedication(String medicationId) {
        return medications.findById(medicationId)
                .map(TreatmentMapper::toResult)
                .orElseThrow(() -> new TreatmentApplicationException(
                        TreatmentApplicationException.Code.MEDICATION_NOT_FOUND, "medication not found"));
    }

    @Override
    public TreatmentResult getTreatment(String caregiverId, String treatmentId) {
        var treatment = treatments.findById(treatmentId)
                .orElseThrow(() -> new TreatmentApplicationException(
                        TreatmentApplicationException.Code.TREATMENT_NOT_FOUND, "treatment not found"));
        if (caregiverId == null || caregiverId.isBlank()
                || !careLinkVerificationPort.isAuthorized(caregiverId, treatment.olderAdultId())) {
            throw new TreatmentApplicationException(
                    TreatmentApplicationException.Code.CARE_LINK_NOT_AUTHORIZED,
                    "treatment is not available to this caregiver"
            );
        }
        return TreatmentMapper.toResult(treatment);
    }
}
