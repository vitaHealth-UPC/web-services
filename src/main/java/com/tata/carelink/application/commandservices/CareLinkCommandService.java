package com.tata.carelink.application.commandservices;

import com.tata.carelink.application.models.CareLinkResult;
import com.tata.carelink.application.models.OlderAdultProfileResult;
import com.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.tata.carelink.domain.model.commands.GenerateLinkingCodeCommand;
import com.tata.carelink.domain.model.commands.RegisterConsentCommand;
import com.tata.carelink.domain.model.commands.RegisterOlderAdultProfileCommand;

public interface CareLinkCommandService {
    OlderAdultProfileResult registerOlderAdult(RegisterOlderAdultProfileCommand command);
    CareLinkResult generateLinkingCode(GenerateLinkingCodeCommand command);
    CareLinkResult accept(AcceptCareLinkCommand command);
    CareLinkResult registerConsent(RegisterConsentCommand command);
}
