package com.lutiano.api.models.response.earnings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record EarningsOverviewTopEarnerResponse(
    Boolean active,
    @JsonProperty("adjustments_xaf") Long adjustmentsXaf,
    @JsonProperty("bonus_xaf") Long bonusXaf,
    @JsonProperty("carried_xaf") Long carriedXaf,
    @JsonProperty("commission_xaf") Long commissionXaf,
    @JsonProperty("payable_xaf") Long payableXaf,
    @JsonProperty("period_id") String periodId,
    @JsonProperty("sales_count") Long salesCount,
    @JsonProperty("sales_xaf") Long salesXaf,
    @JsonProperty("seller_id") String sellerId,
    @JsonProperty("seller_name") String sellerName,
    @JsonProperty("seller_role") String sellerRole,
    @JsonProperty("total_xaf") Long totalXaf,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
