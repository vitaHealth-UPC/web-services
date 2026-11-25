package com.vitahealth.tata.carelink.infrastructure.acl;

import com.vitahealth.tata.carelink.application.internal.outboundservices.AdultDevicePort;
import com.vitahealth.tata.identitysubscription.interfaces.acl.IdentityContextFacade;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Delegates adult credential questions to Identity without reading its persistence. */
@Component
public class IdentityAdultDeviceAdapter implements AdultDevicePort {
  private final IdentityContextFacade identity;

  /**
   * Injects the Identity public boundary lazily: Identity reads Care Link to validate adult
   * sessions, so an eager reference would form a bean cycle.
   */
  public IdentityAdultDeviceAdapter(@Lazy IdentityContextFacade identity) {
    this.identity = identity;
  }

  @Override
  public boolean hasPin(String olderAdultId) {
    return identity.hasPinCredential(olderAdultId);
  }

  @Override
  public boolean hasActiveSession(String olderAdultId) {
    return identity.hasActiveOlderAdultSession(olderAdultId);
  }

  @Override
  public Optional<String> caregiverName(String caregiverId) {
    return identity.accountName(caregiverId);
  }

  @Override
  public void removePin(String olderAdultId) {
    identity.removePinCredential(olderAdultId);
  }

  @Override
  public Set<String> withActiveSession(Collection<String> olderAdultIds) {
    return identity.olderAdultsWithActiveSession(olderAdultIds);
  }
}
