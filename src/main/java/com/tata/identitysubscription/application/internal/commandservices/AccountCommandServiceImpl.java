package com.tata.identitysubscription.application.internal.commandservices;

import com.tata.identitysubscription.application.commandservices.AccountCommandService;
import com.tata.identitysubscription.application.internal.IdentityApplicationException;
import com.tata.identitysubscription.application.internal.outboundservices.PasswordHasher;
import com.tata.identitysubscription.application.internal.outboundservices.SessionTokenService;
import com.tata.identitysubscription.application.internal.outboundservices.VerificationCodeGenerator;
import com.tata.identitysubscription.application.internal.outboundservices.VerificationDeliveryPort;
import com.tata.identitysubscription.application.models.AccountResult;
import com.tata.identitysubscription.application.models.SessionResult;
import com.tata.identitysubscription.domain.model.aggregates.Account;
import com.tata.identitysubscription.domain.model.commands.AuthenticateFamilyCommand;
import com.tata.identitysubscription.domain.model.commands.RegisterFamilyAccountCommand;
import com.tata.identitysubscription.domain.model.commands.RequestNewVerificationCommand;
import com.tata.identitysubscription.domain.model.commands.VerifyEmailCommand;
import com.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.tata.identitysubscription.domain.repositories.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

@Service
@Transactional
public class AccountCommandServiceImpl implements AccountCommandService {
    private static final Duration VERIFICATION_TTL = Duration.ofMinutes(15);

    private final AccountRepository accountRepository;
    private final PasswordHasher passwordHasher;
    private final VerificationCodeGenerator verificationCodeGenerator;
    private final VerificationDeliveryPort verificationDeliveryPort;
    private final SessionTokenService sessionTokenService;
    private final Clock clock;

    public AccountCommandServiceImpl(
            AccountRepository accountRepository,
            PasswordHasher passwordHasher,
            VerificationCodeGenerator verificationCodeGenerator,
            VerificationDeliveryPort verificationDeliveryPort,
            SessionTokenService sessionTokenService
    ) {
        this(accountRepository, passwordHasher, verificationCodeGenerator, verificationDeliveryPort,
                sessionTokenService, Clock.systemUTC());
    }

    AccountCommandServiceImpl(
            AccountRepository accountRepository,
            PasswordHasher passwordHasher,
            VerificationCodeGenerator verificationCodeGenerator,
            VerificationDeliveryPort verificationDeliveryPort,
            SessionTokenService sessionTokenService,
            Clock clock
    ) {
        this.accountRepository = accountRepository;
        this.passwordHasher = passwordHasher;
        this.verificationCodeGenerator = verificationCodeGenerator;
        this.verificationDeliveryPort = verificationDeliveryPort;
        this.sessionTokenService = sessionTokenService;
        this.clock = clock;
    }

    @Override
    public AccountResult register(RegisterFamilyAccountCommand command) {
        var email = new EmailAddress(command.email());
        if (accountRepository.findByEmail(email.value()).isPresent()) {
            throw error(IdentityApplicationException.Code.DUPLICATE_EMAIL, "email already registered");
        }
        if (command.password() == null || command.password().length() < 8) {
            throw new IllegalArgumentException("password must contain at least 8 characters");
        }

        var verificationCode = verificationCodeGenerator.generate();
        var account = Account.register(
                command.name(),
                email,
                passwordHasher.hash(command.password()),
                passwordHasher.hash(verificationCode),
                clock.instant(),
                VERIFICATION_TTL
        );

        var saved = accountRepository.save(account);
        verificationDeliveryPort.send(saved.email().value(), verificationCode);
        return toResult(saved);
    }

    @Override
    public AccountResult verify(VerifyEmailCommand command) {
        var email = new EmailAddress(command.email());
        var account = accountRepository.findByEmail(email.value())
                .orElseThrow(() -> error(IdentityApplicationException.Code.ACCOUNT_NOT_FOUND, "account not found"));

        if (!account.canCompleteVerification(clock.instant())) {
            throw error(IdentityApplicationException.Code.VERIFICATION_EXPIRED, "verification code expired");
        }
        if (command.code() == null || !passwordHasher.matches(command.code(), account.verificationCodeHash())) {
            throw error(IdentityApplicationException.Code.INVALID_VERIFICATION, "verification code is invalid");
        }

        account.completeVerification();
        return toResult(accountRepository.save(account));
    }

    @Override
    public AccountResult requestNewVerification(RequestNewVerificationCommand command) {
        var email = new EmailAddress(command.email());
        var account = accountRepository.findByEmail(email.value())
                .orElseThrow(() -> error(IdentityApplicationException.Code.ACCOUNT_NOT_FOUND, "account not found"));

        if (account.status() != com.tata.identitysubscription.domain.model.valueobjects.AccountStatus.PENDING_VERIFICATION) {
            throw error(IdentityApplicationException.Code.ACCOUNT_NOT_ACTIVE, "account is not pending verification");
        }

        var verificationCode = verificationCodeGenerator.generate();
        account.renewVerificationCode(
                passwordHasher.hash(verificationCode),
                clock.instant(),
                VERIFICATION_TTL
        );

        var saved = accountRepository.save(account);
        verificationDeliveryPort.send(saved.email().value(), verificationCode);
        return toResult(saved);
    }

    @Override
    public SessionResult authenticate(AuthenticateFamilyCommand command) {
        var email = new EmailAddress(command.email());
        var account = accountRepository.findByEmail(email.value())
                .orElseThrow(() -> error(IdentityApplicationException.Code.INVALID_CREDENTIALS, "invalid credentials"));

        if (!account.canAuthenticate()) {
            throw error(IdentityApplicationException.Code.ACCOUNT_NOT_ACTIVE, "account is not active");
        }
        if (command.password() == null || !passwordHasher.matches(command.password(), account.passwordHash())) {
            throw error(IdentityApplicationException.Code.INVALID_CREDENTIALS, "invalid credentials");
        }
        return sessionTokenService.issue(account.id());
    }

    private static AccountResult toResult(Account account) {
        return new AccountResult(account.id(), account.name(), account.email().value(), account.status());
    }

    private static IdentityApplicationException error(IdentityApplicationException.Code code, String message) {
        return new IdentityApplicationException(code, message);
    }
}
