package com.lutiano.api.models.response.earnings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record EarningsOverviewBalanceResponse(
    @JsonProperty("outstanding_xaf") Long outstandingXaf,
    @JsonProperty("owed_xaf") Long owedXaf,
    @JsonProperty("paid_xaf") Long paidXaf,
    @JsonProperty("pending_xaf") Long pendingXaf,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
