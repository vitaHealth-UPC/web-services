package com.tata.treatmentmanagement.application.internal.commandservices;

import com.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.tata.treatmentmanagement.application.commandservices.TreatmentCommandService;
import com.tata.treatmentmanagement.application.events.TreatmentScheduleChangedEvent;
import com.tata.treatmentmanagement.application.events.TreatmentScheduleEventPublisher;
import com.tata.treatmentmanagement.application.internal.TreatmentMapper;
import com.tata.treatmentmanagement.application.models.MedicationResult;
import com.tata.treatmentmanagement.application.models.TreatmentResult;
import com.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.tata.treatmentmanagement.domain.model.commands.*;
import com.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRegimen;
import com.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import com.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import com.tata.treatmentmanagement.domain.services.ICareLinkVerificationPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
public class TreatmentCommandServiceImpl implements TreatmentCommandService {
    private final MedicationRepository medicationRepository;
    private final TreatmentRepository treatmentRepository;
    private final ICareLinkVerificationPort careLinkVerificationPort;
    private final TreatmentScheduleEventPublisher scheduleEventPublisher;
    private final Clock clock;

    public TreatmentCommandServiceImpl(
            MedicationRepository medicationRepository,
            TreatmentRepository treatmentRepository,
            ICareLinkVerificationPort careLinkVerificationPort
    ) {
        this(
                medicationRepository,
                treatmentRepository,
                careLinkVerificationPort,
                event -> {}
        );
    }

    public TreatmentCommandServiceImpl(
            MedicationRepository medicationRepository,
            TreatmentRepository treatmentRepository,
            ICareLinkVerificationPort careLinkVerificationPort,
            TreatmentScheduleEventPublisher scheduleEventPublisher
    ) {
        this(medicationRepository, treatmentRepository, careLinkVerificationPort,
                scheduleEventPublisher, Clock.systemUTC());
    }

    @Autowired
    public TreatmentCommandServiceImpl(
            MedicationRepository medicationRepository,
            TreatmentRepository treatmentRepository,
            ICareLinkVerificationPort careLinkVerificationPort,
            TreatmentScheduleEventPublisher scheduleEventPublisher,
            Clock clock
    ) {
        this.clock = java.util.Objects.requireNonNull(clock);
        this.medicationRepository = medicationRepository;
        this.treatmentRepository = treatmentRepository;
        this.careLinkVerificationPort = careLinkVerificationPort;
        this.scheduleEventPublisher = scheduleEventPublisher;
    }

    @Override
    public MedicationResult registerMedication(RegisterMedicationCommand command) {
        requireAuthorizedCareLink(command.caregiverId(), command.olderAdultId());
        var medication = Medication.register(
                command.olderAdultId(), command.name(), command.presentation(), clock.instant()
        );
        return TreatmentMapper.toResult(medicationRepository.save(medication));
    }

    @Override
    public MedicationResult updateMedication(UpdateMedicationCommand command) {
        var medication = medication(command.medicationId());
        requireAuthorizedCareLink(command.caregiverId(), medication.olderAdultId());
        try {
            medication.update(command.name(), command.presentation());
        } catch (IllegalStateException exception) {
            throw error(TreatmentApplicationException.Code.MEDICATION_INACTIVE, exception.getMessage());
        }
        var saved = medicationRepository.save(medication);
        // the schedule event carries the medication name, so treatments using it must be republished
        treatmentRepository.findByMedicationId(saved.id()).forEach(this::publishSchedule);
        return TreatmentMapper.toResult(saved);
    }

    @Override
    public MedicationResult deactivateMedication(DeactivateMedicationCommand command) {
        var medication = medication(command.medicationId());
        requireAuthorizedCareLink(command.caregiverId(), medication.olderAdultId());
        medication.deactivate();
        var saved = medicationRepository.save(medication);
        // an active treatment cannot keep running with a medication that is no longer active
        for (var treatment : treatmentRepository.findByMedicationId(saved.id())) {
            if (treatment.status() == TreatmentStatus.ACTIVE) {
                treatment.pause();
                publishSchedule(treatmentRepository.save(treatment));
            }
        }
        return TreatmentMapper.toResult(saved);
    }

    @Override
    public TreatmentResult createTreatment(CreateTreatmentCommand command) {
        requireAuthorizedCareLink(command.caregiverId(), command.olderAdultId());
        return TreatmentMapper.toResult(
                treatmentRepository.save(Treatment.create(command.olderAdultId(), command.name(), clock.instant()))
        );
    }

    @Override
    public TreatmentResult configureTreatment(ConfigureTreatmentCommand command) {
        var treatment = treatment(command.treatmentId());
        requireAuthorizedCareLink(command.caregiverId(), treatment.olderAdultId());
        var medication = medication(command.medicationId());
        if (!medication.active()) {
            throw error(TreatmentApplicationException.Code.MEDICATION_INACTIVE, "medication is inactive");
        }
        if (!medication.olderAdultId().equals(treatment.olderAdultId())) {
            throw new IllegalArgumentException("medication and treatment must belong to the same older adult");
        }
        treatment.configure(new TreatmentRegimen(
                command.medicationId(),
                command.dose(),
                command.frequency(),
                command.scheduledTimes(),
                command.instructions(),
                command.reminderLeadMinutes()
        ));
        var saved = treatmentRepository.save(treatment);
        publishSchedule(saved);
        return TreatmentMapper.toResult(saved);
    }

    @Override
    public TreatmentResult activate(ChangeTreatmentStatusCommand command) {
        var treatment = treatment(command.treatmentId());
        requireAuthorizedCareLink(command.caregiverId(), treatment.olderAdultId());
        requireActiveMedication(treatment);
        try {
            treatment.activate();
        } catch (IllegalStateException exception) {
            throw error(TreatmentApplicationException.Code.INCOMPLETE_TREATMENT, exception.getMessage());
        }
        var saved = treatmentRepository.save(treatment);
        publishSchedule(saved);
        return TreatmentMapper.toResult(saved);
    }

    @Override
    public TreatmentResult pause(ChangeTreatmentStatusCommand command) {
        var treatment = treatment(command.treatmentId());
        requireAuthorizedCareLink(command.caregiverId(), treatment.olderAdultId());
        try {
            treatment.pause();
        } catch (IllegalStateException exception) {
            throw error(TreatmentApplicationException.Code.INVALID_TRANSITION, exception.getMessage());
        }
        var saved = treatmentRepository.save(treatment);
        publishSchedule(saved);
        return TreatmentMapper.toResult(saved);
    }

    @Override
    public TreatmentResult resume(ChangeTreatmentStatusCommand command) {
        var treatment = treatment(command.treatmentId());
        requireAuthorizedCareLink(command.caregiverId(), treatment.olderAdultId());
        requireActiveMedication(treatment);
        try {
            treatment.resume();
        } catch (IllegalStateException exception) {
            throw error(TreatmentApplicationException.Code.INVALID_TRANSITION, exception.getMessage());
        }
        var saved = treatmentRepository.save(treatment);
        publishSchedule(saved);
        return TreatmentMapper.toResult(saved);
    }

    private void publishSchedule(Treatment treatment) {
        var regimen = treatment.regimen();
        if (regimen == null) {
            return;
        }
        var medication = medication(regimen.medicationId());
        scheduleEventPublisher.publish(new TreatmentScheduleChangedEvent(
                treatment.id(),
                regimen.medicationId(),
                treatment.olderAdultId(),
                medication.name(),
                regimen.dose(),
                regimen.frequency(),
                regimen.scheduledTimes(),
                regimen.instructions(),
                regimen.reminderLeadMinutes(),
                treatment.status() == TreatmentStatus.ACTIVE
        ));
    }

    private void requireActiveMedication(Treatment treatment) {
        if (treatment.regimen() != null && !medication(treatment.regimen().medicationId()).active()) {
            throw error(TreatmentApplicationException.Code.MEDICATION_INACTIVE, "medication is inactive");
        }
    }

    private Medication medication(String id) {
        return medicationRepository.findById(id)
                .orElseThrow(() -> error(TreatmentApplicationException.Code.MEDICATION_NOT_FOUND, "medication not found"));
    }

    private Treatment treatment(String id) {
        return treatmentRepository.findById(id)
                .orElseThrow(() -> error(TreatmentApplicationException.Code.TREATMENT_NOT_FOUND, "treatment not found"));
    }

    private void requireAuthorizedCareLink(String caregiverId, String olderAdultId) {
        if (caregiverId == null || caregiverId.isBlank()
                || olderAdultId == null || olderAdultId.isBlank()
                || !careLinkVerificationPort.isAuthorized(caregiverId, olderAdultId)) {
            throw error(
                    TreatmentApplicationException.Code.CARE_LINK_NOT_AUTHORIZED,
                    "an active care link is required for this older adult"
            );
        }
    }

    private static TreatmentApplicationException error(TreatmentApplicationException.Code code, String message) {
        return new TreatmentApplicationException(code, message);
    }
}
