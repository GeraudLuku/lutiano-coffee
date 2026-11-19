package com.lutiano.api.models.response.earnings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record StatementItemResponse(
    @JsonProperty("adjustments_xaf") Long adjustmentsXaf,
    @JsonProperty("bonus_xaf") Long bonusXaf,
    @JsonProperty("carried_xaf") Long carriedXaf,
    @JsonProperty("commission_xaf") Long commissionXaf,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("ends_on") String endsOn,
    @JsonProperty("payable_xaf") Long payableXaf,
    @JsonProperty("period_id") String periodId,
    @JsonProperty("sales_count") Long salesCount,
    @JsonProperty("sales_xaf") Long salesXaf,
    @JsonProperty("seller_id") String sellerId,
    @JsonProperty("starts_on") String startsOn,
    @JsonProperty("total_xaf") Long totalXaf,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
