package com.lutiano.api.models.response.cash;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record SummaryResponse(
    @JsonProperty("cash_in_open_shifts_xaf") Long cashInOpenShiftsXaf,
    @JsonProperty("disputed_remittances") Long disputedRemittances,
    @JsonProperty("open_shifts") Long openShifts,
    @JsonProperty("outstanding_remittances") Long outstandingRemittances,
    @JsonProperty("outstanding_remittances_xaf") Long outstandingRemittancesXaf,
    @JsonProperty("pending_outflows") Long pendingOutflows,
    @JsonProperty("shifts_to_review") Long shiftsToReview,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
