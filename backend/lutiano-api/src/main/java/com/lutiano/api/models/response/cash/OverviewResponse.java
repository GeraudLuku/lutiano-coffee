package com.lutiano.api.models.response.cash;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record OverviewResponse(
    @JsonProperty("disputed_remittances") Long disputedRemittances,
    @JsonProperty("outstanding_remittances") Long outstandingRemittances,
    @JsonProperty("outstanding_remittances_xaf") Long outstandingRemittancesXaf,
    @JsonProperty("pending_outflows") Long pendingOutflows,
    @JsonProperty("recounts_requested") Long recountsRequested,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
