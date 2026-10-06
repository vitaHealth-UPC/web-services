package com.tata.adherenceanalytics.interfaces.rest;

import com.tata.intakeexecution.infrastructure.persistence.jpa.repositories.IntakeJpaRepository;
import com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import java.time.*;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/older-adults/{olderAdultId}/adherence")
public class AdherenceController {
  private final IntakeJpaRepository repository;
  public AdherenceController(IntakeJpaRepository repository) { this.repository = repository; }

  @GetMapping("/weekly")
  public WeeklyAdherenceResource weekly(@PathVariable String olderAdultId,
      @RequestParam Instant from, @RequestParam Instant to) {
    if (olderAdultId == null || olderAdultId.isBlank() || from == null || to == null || !to.isAfter(from)
        || Duration.between(from, to).compareTo(Duration.ofDays(8)) > 0) {
      throw new IllegalArgumentException("adherence range must be ordered and at most eight days");
    }
    var intakes = repository.findAgenda(olderAdultId.trim(), from, to);
    var definitive = intakes.stream().filter(i -> i.getStatus() != IntakeStatus.PENDING).toList();
    var confirmed = definitive.stream().filter(i -> i.getStatus() == IntakeStatus.CONFIRMED || i.getStatus() == IntakeStatus.LATE).count();
    double percentage = definitive.isEmpty() ? 0d : (confirmed * 100d) / definitive.size();
    return new WeeklyAdherenceResource(olderAdultId.trim(), from, to, (int) confirmed, definitive.size(), percentage);
  }
}

