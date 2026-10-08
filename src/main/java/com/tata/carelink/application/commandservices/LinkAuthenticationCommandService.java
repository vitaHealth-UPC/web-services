package com.tata.carelink.application.commandservices;
import com.tata.carelink.application.models.AuthenticatedCareLinkResult;
import com.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.tata.carelink.domain.model.commands.RegisterConsentCommand;
public interface LinkAuthenticationCommandService {
 AuthenticatedCareLinkResult accept(AcceptCareLinkCommand command);
 AuthenticatedCareLinkResult registerConsent(RegisterConsentCommand command);
}
