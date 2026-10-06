package com.tata.treatmentmanagement.application.commandservices;

import com.tata.treatmentmanagement.application.models.MedicationResult;
import com.tata.treatmentmanagement.application.models.TreatmentResult;
import com.tata.treatmentmanagement.domain.model.commands.*;

public interface TreatmentCommandService {
    MedicationResult registerMedication(RegisterMedicationCommand command);
    MedicationResult updateMedication(UpdateMedicationCommand command);
    MedicationResult deactivateMedication(DeactivateMedicationCommand command);
    TreatmentResult createTreatment(CreateTreatmentCommand command);
    TreatmentResult configureTreatment(ConfigureTreatmentCommand command);
    TreatmentResult activate(ChangeTreatmentStatusCommand command);
    TreatmentResult pause(ChangeTreatmentStatusCommand command);
    TreatmentResult resume(ChangeTreatmentStatusCommand command);
}
