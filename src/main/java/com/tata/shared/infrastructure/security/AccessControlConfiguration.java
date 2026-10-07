package com.tata.shared.infrastructure.security;

import com.tata.carelink.application.queryservices.CareLinkQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccessControlConfiguration {
    @Bean
    OlderAdultAccessPort olderAdultAccessPort(CareLinkQueryService careLinks) {
        return (subjectId, olderAdultId) ->
                subjectId != null
                        && olderAdultId != null
                        && (subjectId.equals(olderAdultId.trim())
                        || careLinks.isAuthorized(subjectId, olderAdultId.trim()));
    }
}
