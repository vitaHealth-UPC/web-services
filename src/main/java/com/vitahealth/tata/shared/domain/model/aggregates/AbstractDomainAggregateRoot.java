package com.vitahealth.tata.shared.domain.model.aggregates;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Buffers domain events until an application operation takes them for publication. Identity,
 * persistence and transaction boundaries belong to the concrete context.
 */
public abstract class AbstractDomainAggregateRoot {
  private final List<Object> domainEvents = new ArrayList<>();

  /** Records an event without publishing it or changing persistence state. */
  protected final void registerDomainEvent(Object event) {
    domainEvents.add(Objects.requireNonNull(event, "event"));
  }

  /** Returns a defensive snapshot and clears the pending buffer exactly once. */
  public final List<Object> pullDomainEvents() {
    var events = List.copyOf(domainEvents);
    domainEvents.clear();
    return events;
  }
}
