package com.vitahealth.tata.familymonitoring.application.internal.queryservices;

import com.vitahealth.tata.familymonitoring.application.queryservices.GetContactChannelQueryService;
import com.vitahealth.tata.familymonitoring.domain.exceptions.ContactChannelNotAvailableException;
import com.vitahealth.tata.familymonitoring.domain.model.queries.GetContactChannelQuery;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.ContactChannel;
import com.vitahealth.tata.familymonitoring.domain.ports.ContactChannelPort;
import org.springframework.stereotype.Service;

/** Reads adult evidence after selecting the requesting caregiver's follow-up. */
@Service
public class GetContactChannelQueryHandler implements GetContactChannelQueryService {

  private final FamilyMonitorSelectionService selection;
  private final ContactChannelPort contactChannelPort;

  /** Injects follow-up storage and the public source-context query ports. */
  public GetContactChannelQueryHandler(
      FamilyMonitorSelectionService selection, ContactChannelPort contactChannelPort) {
    this.selection = selection;
    this.contactChannelPort = contactChannelPort;
  }

  public ContactChannel handle(GetContactChannelQuery query) {
    selection.requireCurrent(query.olderAdultId(), query.caregiverId());
    return contactChannelPort
        .findContactChannel(query.olderAdultId())
        .orElseThrow(() -> new ContactChannelNotAvailableException(query.olderAdultId()));
  }
}
