package com.lutiano.api.models.response.earnings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record EarningsOverviewPeriodTotalsResponse(
    @JsonProperty("adjustments_xaf") Long adjustmentsXaf,
    @JsonProperty("bonus_xaf") Long bonusXaf,
    @JsonProperty("commission_xaf") Long commissionXaf,
    @JsonProperty("payable_xaf") Long payableXaf,
    Long sellers,
    @JsonProperty("total_xaf") Long totalXaf,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
