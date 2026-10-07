package com.tata.intakeexecution.application.acl;
import com.tata.intakeexecution.interfaces.acl.IntakeContextFacade;
import com.tata.intakeexecution.application.models.IntakeResult;
import com.tata.intakeexecution.application.internal.IntakeMapper;
import com.tata.intakeexecution.application.internal.queryservices.GetIntakeHistoryQueryHandler;
import com.tata.intakeexecution.application.internal.queryservices.GetNextIntakeQueryHandler;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly=true)
public class IntakeContextFacadeImpl implements IntakeContextFacade {
 private final IntakeRepository repository;
 private final GetIntakeHistoryQueryHandler history;
 private final GetNextIntakeQueryHandler next;
 public IntakeContextFacadeImpl(IntakeRepository repository, GetIntakeHistoryQueryHandler history, GetNextIntakeQueryHandler next) {
  this.repository=repository;this.history=history;this.next=next;
 }
 @Override public List<IntakeResult> findEvidence(String owner,Instant from,Instant to) {
  return repository.findAgenda(owner,from,to).stream().map(IntakeMapper::toResult).toList();
 }
 @Override public List<String> findOlderAdultIdsWithIntakes(Instant from,Instant to) { return repository.findOlderAdultIdsWithIntakes(from,to); }
 @Override public List<IntakeResult> history(String owner,Instant from,Instant to) { return history.handle(owner,from,to); }
 @Override public Optional<IntakeResult> next(String owner) { return next.handle(owner); }
 @Override public Optional<IntakeResult> findIntake(String id) { return repository.findById(id).map(IntakeMapper::toResult); }
}
