package com.tata.familymonitoring.domain.model.queries;

public record GetRecentIntakeHistoryQuery(Long olderAdultId, int days) {
}
