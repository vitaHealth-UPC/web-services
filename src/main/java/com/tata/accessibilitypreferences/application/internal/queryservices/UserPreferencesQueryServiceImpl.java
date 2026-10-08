package com.tata.accessibilitypreferences.application.internal.queryservices;

import com.tata.accessibilitypreferences.application.models.NotificationPreferencesResult;
import com.tata.accessibilitypreferences.application.queryservices.UserPreferencesQueryService;
import com.tata.accessibilitypreferences.domain.factories.UserPreferencesFactory;
import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.tata.accessibilitypreferences.domain.model.valueobjects.ChannelType;
import com.tata.accessibilitypreferences.domain.repositories.IUserPreferencesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class UserPreferencesQueryServiceImpl implements UserPreferencesQueryService {
    private final IUserPreferencesRepository repository;

    public UserPreferencesQueryServiceImpl(IUserPreferencesRepository repository) {
        this.repository = repository;
    }

    @Override
    public NotificationPreferencesResult getNotificationPreferences(String userId) {
        var preferences = preferencesOf(userId);
        var quietHours = preferences.quietHours();
        return new NotificationPreferencesResult(
                preferences.isChannelEnabled(ChannelType.PUSH),
                preferences.isChannelEnabled(ChannelType.SMS),
                preferences.isChannelEnabled(ChannelType.EMAIL),
                quietHours == null ? null : quietHours.start(),
                quietHours == null ? null : quietHours.end());
    }

    @Override
    public boolean isVoiceConfirmationEnabled(String userId) {
        return preferencesOf(userId).voiceConfirmationEnabled();
    }

    private UserPreferences preferencesOf(String userId) {
        return repository.findByUserId(userId)
                .orElseGet(() -> UserPreferencesFactory.createDefaults(userId, Instant.now()));
    }
}
