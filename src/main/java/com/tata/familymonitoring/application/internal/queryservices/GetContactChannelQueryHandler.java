package com.tata.familymonitoring.application.internal.queryservices;

import com.tata.familymonitoring.domain.exceptions.ContactChannelNotAvailableException;
import com.tata.familymonitoring.domain.exceptions.FamilyMonitorNotFoundException;
import com.tata.familymonitoring.domain.model.queries.GetContactChannelQuery;
import com.tata.familymonitoring.domain.model.valueobjects.ContactChannel;
import com.tata.familymonitoring.domain.ports.IContactChannelPort;
import com.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import org.springframework.stereotype.Service;

@Service
public class GetContactChannelQueryHandler {

  private final IFamilyMonitorRepository repository;
  private final IContactChannelPort contactChannelPort;

  public GetContactChannelQueryHandler(
      IFamilyMonitorRepository repository, IContactChannelPort contactChannelPort) {
    this.repository = repository;
    this.contactChannelPort = contactChannelPort;
  }

  public ContactChannel handle(GetContactChannelQuery query) {
    repository.findByOlderAdultId(query.olderAdultId())
        .orElseThrow(() -> new FamilyMonitorNotFoundException(query.olderAdultId()));
    return contactChannelPort.findContactChannel(query.olderAdultId())
        .orElseThrow(() -> new ContactChannelNotAvailableException(query.olderAdultId()));
  }
}
