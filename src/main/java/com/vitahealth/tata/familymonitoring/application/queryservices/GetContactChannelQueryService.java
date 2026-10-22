package com.vitahealth.tata.familymonitoring.application.queryservices;

import com.vitahealth.tata.familymonitoring.domain.model.queries.GetContactChannelQuery;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.ContactChannel;

/** Resolves the authorized adult contact channel without performing an external call. */
public interface GetContactChannelQueryService {
  ContactChannel handle(GetContactChannelQuery query);
}
