package com.tata.identitysubscription.infrastructure.security;
import com.tata.identitysubscription.application.internal.outboundservices.SessionTokenService;
import com.tata.identitysubscription.application.models.SessionResult;
import com.tata.identitysubscription.application.models.AuthenticatedSession;
import com.tata.identitysubscription.application.models.AuthenticatedSession.Role;
import com.tata.identitysubscription.domain.repositories.AccountRepository;
import com.tata.identitysubscription.infrastructure.persistence.jpa.entities.AccountSessionPersistenceEntity;
import com.tata.identitysubscription.infrastructure.persistence.jpa.repositories.AccountSessionJpaRepository;
import com.tata.carelink.interfaces.acl.CareLinkContextFacade;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional
public class OpaqueSessionTokenService implements SessionTokenService {
 private final AccountSessionJpaRepository sessions;
 private final AccountRepository accounts;
 private final CareLinkContextFacade links;
 public OpaqueSessionTokenService(AccountSessionJpaRepository sessions, AccountRepository accounts, CareLinkContextFacade links) {
  this.sessions=sessions; this.accounts=accounts; this.links=links;
 }
 public SessionResult issue(String accountId) { return issue(accountId, Role.CAREGIVER, null, 720); }
 public SessionResult issueLinkSetup(String owner, String linkId) { return issue(owner,Role.LINK_SETUP,linkId,15); }
 public SessionResult issueOlderAdult(String owner) {
  var profile=links.findOlderAdult(owner).orElseThrow(() -> new IllegalArgumentException("older adult profile not found"));
  var link=links.getConfirmedByCaregiver(profile.registeredByCaregiverId()).stream().filter(l -> owner.equals(l.olderAdultId())).findFirst()
   .orElseThrow(() -> new IllegalArgumentException("a confirmed care link is required"));
  return issue(owner,Role.OLDER_ADULT,link.id(),720);
 }
 private SessionResult issue(String owner, Role role, String linkId, int minutes) {
  var raw=UUID.randomUUID()+"."+UUID.randomUUID(); var expiry=Instant.now().plus(minutes,ChronoUnit.MINUTES);
  sessions.save(new AccountSessionPersistenceEntity(owner,sha256(raw),expiry,role.name(),linkId));
  return new SessionResult(owner,raw,expiry);
 }
 @Transactional(readOnly=true)
 public Optional<AuthenticatedSession> authenticate(String token) {
  if(token==null || token.isBlank() || token.length()>256) return Optional.empty();
  return sessions.findByTokenHash(sha256(token)).filter(s -> s.getExpiresAt().isAfter(Instant.now())).flatMap(s -> {
   if(s.getRole()==null) return Optional.empty();
   final Role role; try { role=Role.valueOf(s.getRole()); } catch(IllegalArgumentException ex) { return Optional.empty(); }
   if(role==Role.CAREGIVER) {
    if(accounts.findById(s.getAccountId()).filter(a -> a.canAuthenticate()).isEmpty()) return Optional.empty();
   } else {
    if(s.getCareLinkId()==null) return Optional.empty();
    var link=links.getById(s.getCareLinkId());
    if(!s.getAccountId().equals(link.olderAdultId()) || accounts.findById(link.caregiverId()).filter(a -> a.canAuthenticate()).isEmpty()) return Optional.empty();
    if(role==Role.OLDER_ADULT && !links.isAuthorized(link.caregiverId(),link.olderAdultId())) return Optional.empty();
    if(role==Role.LINK_SETUP && link.codeUsedAt()==null) return Optional.empty();
   }
   return Optional.of(new AuthenticatedSession(s.getAccountId(),role,s.getExpiresAt(),s.getCareLinkId()));
  });
 }
 public void revoke(String token) { if(token!=null && !token.isBlank()) sessions.deleteByTokenHash(sha256(token)); }
 private static String sha256(String value) {
  try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
  catch(NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 unavailable",ex); }
 }
}
