package com.vitahealth.tata.accessibilitypreferences.application.internal.commandservices;

import com.vitahealth.tata.accessibilitypreferences.application.PreferencesApplicationException;
import com.vitahealth.tata.accessibilitypreferences.application.commandservices.NotificationPreferencesCommandService;
import com.vitahealth.tata.accessibilitypreferences.application.commandservices.UpdateContrastCommandService;
import com.vitahealth.tata.accessibilitypreferences.application.commandservices.UpdateNotificationChannelsCommandService;
import com.vitahealth.tata.accessibilitypreferences.application.commandservices.UpdateNotificationPolicyCommandService;
import com.vitahealth.tata.accessibilitypreferences.application.commandservices.UpdateQuietHoursCommandService;
import com.vitahealth.tata.accessibilitypreferences.application.commandservices.UpdateReadingAssistanceCommandService;
import com.vitahealth.tata.accessibilitypreferences.application.commandservices.UpdateReducedMotionCommandService;
import com.vitahealth.tata.accessibilitypreferences.application.commandservices.UpdateTextSizeCommandService;
import com.vitahealth.tata.accessibilitypreferences.application.commandservices.UpdateVoiceConfirmationCommandService;
import com.vitahealth.tata.accessibilitypreferences.application.result.UserPreferencesResult;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateContrastCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateNotificationChannelsCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateNotificationPolicyCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateNotificationPreferencesCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateQuietHoursCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateReadingAssistanceCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateReducedMotionCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateTextSizeCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateVoiceConfirmationCommand;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/** Captures expected preference rejections only after delegated transactional handlers finish. */
@Service
public class UserPreferencesCommandServiceImpl
    implements UpdateContrastCommandService,
        UpdateNotificationChannelsCommandService,
        UpdateNotificationPolicyCommandService,
        UpdateQuietHoursCommandService,
        UpdateReadingAssistanceCommandService,
        UpdateReducedMotionCommandService,
        UpdateTextSizeCommandService,
        UpdateVoiceConfirmationCommandService,
        NotificationPreferencesCommandService {
  private final UpdateContrastCommandHandler contrast;
  private final UpdateNotificationChannelsCommandHandler channels;
  private final UpdateNotificationPolicyCommandHandler policy;
  private final UpdateQuietHoursCommandHandler quiet;
  private final UpdateReadingAssistanceCommandHandler reading;
  private final UpdateReducedMotionCommandHandler motion;
  private final UpdateTextSizeCommandHandler text;
  private final UpdateVoiceConfirmationCommandHandler voice;
  private final UpdateNotificationPreferencesCommandHandler combined;

  /** Injects transaction-owning handlers for each independent preference decision. */
  public UserPreferencesCommandServiceImpl(
      UpdateContrastCommandHandler contrast,
      UpdateNotificationChannelsCommandHandler channels,
      UpdateNotificationPolicyCommandHandler policy,
      UpdateQuietHoursCommandHandler quiet,
      UpdateReadingAssistanceCommandHandler reading,
      UpdateReducedMotionCommandHandler motion,
      UpdateTextSizeCommandHandler text,
      UpdateVoiceConfirmationCommandHandler voice,
      UpdateNotificationPreferencesCommandHandler combined) {
    this.contrast = contrast;
    this.channels = channels;
    this.policy = policy;
    this.quiet = quiet;
    this.reading = reading;
    this.motion = motion;
    this.text = text;
    this.voice = voice;
    this.combined = combined;
  }

  /** Returns an immutable Contrast outcome after the handler transaction finishes. */
  @Override
  public Result<UserPreferencesResult, ApplicationError> handle(UpdateContrastCommand command) {
    return execute(() -> contrast.handle(command));
  }

  /** Returns an immutable NotificationChannels outcome after the handler transaction finishes. */
  @Override
  public Result<UserPreferencesResult, ApplicationError> handle(
      UpdateNotificationChannelsCommand command) {
    return execute(() -> channels.handle(command));
  }

  /** Returns an immutable NotificationPolicy outcome after the handler transaction finishes. */
  @Override
  public Result<UserPreferencesResult, ApplicationError> handle(
      UpdateNotificationPolicyCommand command) {
    return execute(() -> policy.handle(command));
  }

  /** Returns an immutable QuietHours outcome after the handler transaction finishes. */
  @Override
  public Result<UserPreferencesResult, ApplicationError> handle(UpdateQuietHoursCommand command) {
    return execute(() -> quiet.handle(command));
  }

  /** Returns an immutable ReadingAssistance outcome after the handler transaction finishes. */
  @Override
  public Result<UserPreferencesResult, ApplicationError> handle(
      UpdateReadingAssistanceCommand command) {
    return execute(() -> reading.handle(command));
  }

  /** Returns an immutable ReducedMotion outcome after the handler transaction finishes. */
  @Override
  public Result<UserPreferencesResult, ApplicationError> handle(
      UpdateReducedMotionCommand command) {
    return execute(() -> motion.handle(command));
  }

  /** Returns an immutable TextSize outcome after the handler transaction finishes. */
  @Override
  public Result<UserPreferencesResult, ApplicationError> handle(UpdateTextSizeCommand command) {
    return execute(() -> text.handle(command));
  }

  /** Returns an immutable VoiceConfirmation outcome after the handler transaction finishes. */
  @Override
  public Result<UserPreferencesResult, ApplicationError> handle(
      UpdateVoiceConfirmationCommand command) {
    return execute(() -> voice.handle(command));
  }

  /**
   * Returns an immutable NotificationPreferences outcome after the handler transaction finishes.
   */
  @Override
  public Result<UserPreferencesResult, ApplicationError> update(
      UpdateNotificationPreferencesCommand command) {
    return execute(() -> combined.handle(command));
  }

  private static Result<UserPreferencesResult, ApplicationError> execute(
      Supplier<UserPreferencesResult> operation) {
    try {
      return Result.success(operation.get());
    } catch (PreferencesApplicationException rejectedDecision) {
      return Result.failure(rejectedDecision.toApplicationError());
    }
  }
}
