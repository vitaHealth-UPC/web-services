package com.tata.carelink.application.internal.outboundservices.acl;
import com.tata.carelink.application.internal.outboundservices.LinkSessionPort;
import com.tata.carelink.application.models.AuthenticatedCareLinkResult;
import com.tata.carelink.application.models.CareLinkResult;
import com.tata.identitysubscription.interfaces.acl.IdentityContextFacade;
import org.springframework.stereotype.Component;
@Component
public class IdentityLinkSessionAdapter implements LinkSessionPort {
 private final IdentityContextFacade identity;
 public IdentityLinkSessionAdapter(IdentityContextFacade identity) { this.identity=identity; }
 public AuthenticatedCareLinkResult establish(CareLinkResult link) {
  var session=link.consentGranted() && link.confirmedAt()!=null ? identity.issueOlderAdult(link.olderAdultId()) : identity.issueLinkSetup(link.olderAdultId(),link.id());
  return new AuthenticatedCareLinkResult(link,session.accessToken(),session.expiresAt());
 }
}
