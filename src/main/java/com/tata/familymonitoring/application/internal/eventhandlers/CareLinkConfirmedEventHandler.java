package com.tata.familymonitoring.application.internal.eventhandlers;

import com.tata.carelink.domain.model.events.CareLinkConfirmed;
import com.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates follow-up in the same transaction as consent confirmation. */
@Service
public class CareLinkConfirmedEventHandler {
    private final IFamilyMonitorRepository repository;
    public CareLinkConfirmedEventHandler(IFamilyMonitorRepository repository) { this.repository = repository; }
    @EventListener
    @Transactional
    public void handle(CareLinkConfirmed event) {
        if (repository.findByCareLinkId(event.careLinkId()).isEmpty())
            repository.save(new FamilyMonitor(event.careLinkId(), event.olderAdultId(), event.caregiverId()));
    }
}
