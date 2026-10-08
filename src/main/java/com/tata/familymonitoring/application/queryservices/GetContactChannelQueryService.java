package com.tata.familymonitoring.application.queryservices;

import com.tata.familymonitoring.domain.model.queries.GetContactChannelQuery;
import com.tata.familymonitoring.domain.model.valueobjects.ContactChannel;

public interface GetContactChannelQueryService {
    ContactChannel handle(GetContactChannelQuery query);
}
