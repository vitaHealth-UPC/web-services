package com.tata.accessibilitypreferences.application.internal.queryservices;

import com.tata.accessibilitypreferences.application.internal.commandservices.InitializeDefaultPreferencesCommandHandler;
import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.tata.accessibilitypreferences.domain.model.commands.InitializeDefaultPreferencesCommand;
import com.tata.accessibilitypreferences.domain.model.queries.GetUserPreferencesQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GetUserPreferencesQueryHandler implements com.tata.accessibilitypreferences.application.queryservices.GetUserPreferencesQueryService {
    private final InitializeDefaultPreferencesCommandHandler initializer;

    public GetUserPreferencesQueryHandler(InitializeDefaultPreferencesCommandHandler initializer) {
        this.initializer = initializer;
    }

    /** A user who never saved anything gets the defaults, which are stored on the first read. */
    public UserPreferences handle(GetUserPreferencesQuery query) {
        return initializer.handle(new InitializeDefaultPreferencesCommand(query.userId()));
    }
}
