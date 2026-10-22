package com.vitahealth.tata.intakeexecution.application.internal.queryservices;

import com.vitahealth.tata.intakeexecution.application.IntakeApplicationException;
import com.vitahealth.tata.intakeexecution.application.queryservices.GetIntakeAgendaQueryService;
import com.vitahealth.tata.intakeexecution.application.queryservices.GetIntakeDetailQueryService;
import com.vitahealth.tata.intakeexecution.application.queryservices.GetNextIntakeQueryService;
import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import com.vitahealth.tata.intakeexecution.domain.model.queries.GetIntakeAgendaQuery;
import com.vitahealth.tata.intakeexecution.domain.model.queries.GetIntakeDetailQuery;
import com.vitahealth.tata.intakeexecution.domain.model.queries.GetNextIntakeQuery;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/** Captures public read outcomes after their transactional handlers finish. */
@Service
public class IntakeQueryServiceImpl
    implements GetIntakeAgendaQueryService, GetIntakeDetailQueryService, GetNextIntakeQueryService {
  private final GetIntakeAgendaQueryHandler agenda;
  private final GetIntakeDetailQueryHandler detail;
  private final GetNextIntakeQueryHandler next;

  /** Injects transaction-owning read handlers. */
  public IntakeQueryServiceImpl(
      GetIntakeAgendaQueryHandler agenda,
      GetIntakeDetailQueryHandler detail,
      GetNextIntakeQueryHandler next) {
    this.agenda = agenda;
    this.detail = detail;
    this.next = next;
  }

  /** Returns the selected agenda, including a successful empty collection. */
  @Override
  public Result<List<IntakeResult>, ApplicationError> handle(GetIntakeAgendaQuery query) {
    return execute(() -> agenda.handle(query.olderAdultId(), query.from(), query.to()));
  }

  /** Returns successful valid absence separately from a rejected selection. */
  @Override
  public Result<Optional<IntakeResult>, ApplicationError> handle(GetIntakeDetailQuery query) {
    return execute(() -> detail.handle(query.intakeId()));
  }

  /** Returns the next pending evidence or a successful empty Optional. */
  @Override
  public Result<Optional<IntakeResult>, ApplicationError> handle(GetNextIntakeQuery query) {
    return execute(() -> next.handle(query.olderAdultId()));
  }

  private static <T> Result<T, ApplicationError> execute(Supplier<T> operation) {
    try {
      return Result.success(operation.get());
    } catch (IntakeApplicationException rejection) {
      return Result.failure(rejection.toApplicationError());
    }
  }
}
