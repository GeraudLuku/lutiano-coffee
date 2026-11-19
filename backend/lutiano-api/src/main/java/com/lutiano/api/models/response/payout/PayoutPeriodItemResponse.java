package com.lutiano.api.models.response.payout;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record PayoutPeriodItemResponse(
    @JsonProperty("ends_on") String endsOn,
    @JsonProperty("paid_xaf") Long paidXaf,
    @JsonProperty("payable_xaf") Long payableXaf,
    @JsonProperty("pending_xaf") Long pendingXaf,
    @JsonProperty("period_id") String periodId,
    @JsonProperty("remaining_xaf") Long remainingXaf,
    @JsonProperty("seller_id") String sellerId,
    @JsonProperty("starts_on") String startsOn,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
