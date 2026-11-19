package com.lutiano.api.models.response.earnings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record EarningsOverviewV2Response(
    @JsonProperty("adjustments_xaf") Long adjustmentsXaf,
    EarningsOverviewBalanceResponse balance,
    @JsonProperty("bonus_xaf") Long bonusXaf,
    @JsonProperty("commission_xaf") Long commissionXaf,
    @JsonProperty("ends_on") String endsOn,
    String month,
    @JsonProperty("previous_total_xaf") Long previousTotalXaf,
    @JsonProperty("sales_count") Long salesCount,
    @JsonProperty("sales_xaf") Long salesXaf,
    @JsonProperty("starts_on") String startsOn,
    String status,
    @JsonProperty("total_xaf") Long totalXaf,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
