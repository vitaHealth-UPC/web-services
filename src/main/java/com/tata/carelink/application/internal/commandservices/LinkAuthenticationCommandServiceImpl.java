package com.tata.carelink.application.internal.commandservices;
import com.tata.carelink.application.commandservices.CareLinkCommandService;
import com.tata.carelink.application.commandservices.LinkAuthenticationCommandService;
import com.tata.carelink.application.internal.outboundservices.LinkSessionPort;
import com.tata.carelink.application.models.AuthenticatedCareLinkResult;
import com.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.tata.carelink.domain.model.commands.RegisterConsentCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional
public class LinkAuthenticationCommandServiceImpl implements LinkAuthenticationCommandService {
 private final CareLinkCommandService links;
 private final LinkSessionPort sessions;
 public LinkAuthenticationCommandServiceImpl(CareLinkCommandService links,LinkSessionPort sessions) { this.links=links; this.sessions=sessions; }
 public AuthenticatedCareLinkResult accept(AcceptCareLinkCommand command) { return sessions.establish(links.accept(command)); }
 public AuthenticatedCareLinkResult registerConsent(RegisterConsentCommand command) {
  var link=links.registerConsent(command);
  return command.accepted() ? sessions.establish(link) : new AuthenticatedCareLinkResult(link,null,null);
 }
}
