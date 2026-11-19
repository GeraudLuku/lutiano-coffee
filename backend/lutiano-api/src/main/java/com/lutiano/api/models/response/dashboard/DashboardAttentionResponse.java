package com.lutiano.api.models.response.dashboard;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record DashboardAttentionResponse(
    Long cashVariances,
    Long discrepancies,
    Long inTransit,
    Long lowStockCentral,
    Long lowStockOperations,
    Long pendingApprovals,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
