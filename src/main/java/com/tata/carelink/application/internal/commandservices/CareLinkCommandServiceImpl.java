package com.tata.carelink.application.internal.commandservices;

import com.tata.carelink.application.commandservices.CareLinkCommandService;
import com.tata.carelink.application.internal.CareLinkApplicationException;
import com.tata.carelink.application.internal.CareLinkMapper;
import com.tata.carelink.application.internal.outboundservices.AccountStatusPort;
import com.tata.carelink.application.internal.outboundservices.LinkingCodeGenerator;
import com.tata.carelink.application.models.CareLinkResult;
import com.tata.carelink.application.models.OlderAdultProfileResult;
import com.tata.carelink.domain.model.aggregates.CareLink;
import com.tata.carelink.domain.model.aggregates.OlderAdultProfile;
import com.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.tata.carelink.domain.model.commands.GenerateLinkingCodeCommand;
import com.tata.carelink.domain.model.commands.RegisterConsentCommand;
import com.tata.carelink.domain.model.commands.RegisterOlderAdultProfileCommand;
import com.tata.carelink.domain.model.valueobjects.EmergencyContact;
import com.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.tata.carelink.domain.model.valueobjects.OlderAdultBasicData;
import com.tata.carelink.domain.repositories.CareLinkRepository;
import com.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import com.tata.carelink.domain.services.CareLinkConfirmationPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

@Service
@Transactional
public class CareLinkCommandServiceImpl implements CareLinkCommandService {
    private static final Duration LINKING_CODE_TTL = Duration.ofMinutes(15);

    private final OlderAdultProfileRepository olderAdultRepository;
    private final CareLinkRepository careLinkRepository;
    private final AccountStatusPort accountStatusPort;
    private final LinkingCodeGenerator linkingCodeGenerator;
    private final CareLinkConfirmationPolicy confirmationPolicy;
    private final Clock clock;
    private final org.springframework.context.ApplicationEventPublisher events;

    @Autowired
    public CareLinkCommandServiceImpl(
            OlderAdultProfileRepository olderAdultRepository,
            CareLinkRepository careLinkRepository,
            AccountStatusPort accountStatusPort,
            LinkingCodeGenerator linkingCodeGenerator,
            org.springframework.context.ApplicationEventPublisher events
    ) {
        this(
                olderAdultRepository,
                careLinkRepository,
                accountStatusPort,
                linkingCodeGenerator,
                new CareLinkConfirmationPolicy(),
                Clock.systemUTC(),
                events
        );
    }

    CareLinkCommandServiceImpl(
            OlderAdultProfileRepository olderAdultRepository,
            CareLinkRepository careLinkRepository,
            AccountStatusPort accountStatusPort,
            LinkingCodeGenerator linkingCodeGenerator,
            CareLinkConfirmationPolicy confirmationPolicy,
            Clock clock,
            org.springframework.context.ApplicationEventPublisher events
    ) {
        this.olderAdultRepository = olderAdultRepository;
        this.careLinkRepository = careLinkRepository;
        this.accountStatusPort = accountStatusPort;
        this.linkingCodeGenerator = linkingCodeGenerator;
        this.confirmationPolicy = confirmationPolicy;
        this.clock = clock;
        this.events = events;
    }

    @Override
    public OlderAdultProfileResult registerOlderAdult(RegisterOlderAdultProfileCommand command) {
        requireEnabledAccount(command.caregiverId());

        EmergencyContact contact = null;
        if (hasAnyEmergencyContactValue(command)) {
            contact = new EmergencyContact(
                    command.emergencyContactName(),
                    command.emergencyContactRelationship(),
                    command.emergencyContactPhone()
            );
        }

        var profile = OlderAdultProfile.register(
                command.caregiverId(),
                new OlderAdultBasicData(command.fullName(), command.birthDate()),
                contact,
                clock.instant()
        );
        return CareLinkMapper.toResult(olderAdultRepository.save(profile));
    }

    @Override
    public CareLinkResult generateLinkingCode(GenerateLinkingCodeCommand command) {
        requireEnabledAccount(command.caregiverId());
        var profile = olderAdultRepository.findById(command.olderAdultId())
                .orElseThrow(() -> error(CareLinkApplicationException.Code.OLDER_ADULT_NOT_FOUND, "older adult profile not found"));

        if (!profile.registeredByCaregiverId().equals(command.caregiverId())) {
            throw error(CareLinkApplicationException.Code.OLDER_ADULT_NOT_FOUND, "older adult profile is not available to this caregiver");
        }

        var code = LinkingCode.issue(
                linkingCodeGenerator.generate(),
                clock.instant(),
                LINKING_CODE_TTL
        );
        var careLink = CareLink.createPending(command.caregiverId(), profile.id(), code, clock.instant());
        return CareLinkMapper.toResult(careLinkRepository.save(careLink));
    }

    @Override
    public CareLinkResult accept(AcceptCareLinkCommand command) {
        requireEnabledAccount(command.caregiverId());

        var careLink = careLinkRepository.findByCode(normalizeCode(command.code()))
                .orElseThrow(() -> error(CareLinkApplicationException.Code.INVALID_LINKING_CODE, "linking code is invalid"));

        if (!careLink.caregiverId().equals(command.caregiverId())) {
            throw error(CareLinkApplicationException.Code.INVALID_LINKING_CODE, "linking code is invalid");
        }

        try {
            careLink.accept(command.code(), clock.instant());
        } catch (IllegalStateException exception) {
            throw error(CareLinkApplicationException.Code.LINKING_CODE_EXPIRED_OR_USED, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            throw error(CareLinkApplicationException.Code.INVALID_LINKING_CODE, exception.getMessage());
        }
        return CareLinkMapper.toResult(careLinkRepository.save(careLink));
    }

    @Override
    public CareLinkResult registerConsent(RegisterConsentCommand command) {
        var careLink = careLinkRepository.findById(command.careLinkId())
                .orElseThrow(() -> error(CareLinkApplicationException.Code.CARE_LINK_NOT_FOUND, "care link not found"));

        try {
            careLink.registerConsent(command.accepted(), clock.instant());
        } catch (IllegalStateException exception) {
            throw error(CareLinkApplicationException.Code.CONSENT_REQUIRED, exception.getMessage());
        }

        if (!command.accepted()) {
            return CareLinkMapper.toResult(careLinkRepository.save(careLink));
        }

        if (!confirmationPolicy.canConfirm(careLink, clock.instant())) {
            throw error(CareLinkApplicationException.Code.CONSENT_REQUIRED, "care link cannot be confirmed");
        }

        careLink.confirm(clock.instant());
        var saved = careLinkRepository.save(careLink);
        events.publishEvent(new com.tata.carelink.domain.model.events.CareLinkConfirmed(
                saved.id(), saved.caregiverId(), saved.olderAdultId()));
        return CareLinkMapper.toResult(saved);
    }

    private void requireEnabledAccount(String caregiverId) {
        if (caregiverId == null || caregiverId.isBlank() || !accountStatusPort.isEnabled(caregiverId)) {
            throw error(CareLinkApplicationException.Code.ACCOUNT_NOT_ENABLED, "caregiver account is not enabled");
        }
    }

    private static boolean hasAnyEmergencyContactValue(RegisterOlderAdultProfileCommand command) {
        return notBlank(command.emergencyContactName())
                || notBlank(command.emergencyContactRelationship())
                || notBlank(command.emergencyContactPhone());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw error(CareLinkApplicationException.Code.INVALID_LINKING_CODE, "linking code is required");
        }
        return code.trim().toUpperCase();
    }

    private static CareLinkApplicationException error(CareLinkApplicationException.Code code, String message) {
        return new CareLinkApplicationException(code, message);
    }
}
