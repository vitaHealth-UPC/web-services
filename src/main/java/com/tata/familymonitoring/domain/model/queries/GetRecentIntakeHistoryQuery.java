package com.tata.familymonitoring.domain.model.queries;

public record GetRecentIntakeHistoryQuery(String olderAdultId, int days) {
}
