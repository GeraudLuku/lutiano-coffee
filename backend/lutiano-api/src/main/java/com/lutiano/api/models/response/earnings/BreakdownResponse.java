package com.lutiano.api.models.response.earnings;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record BreakdownResponse(
    @JsonProperty("addon_commission_xaf") Long addonCommissionXaf,
    @JsonProperty("adjustments_xaf") Long adjustmentsXaf,
    @JsonProperty("bonus_xaf") Long bonusXaf,
    @JsonProperty("commission_xaf") Long commissionXaf,
    String from,
    @JsonProperty("product_commission_xaf") Long productCommissionXaf,
    String to,
    @JsonProperty("top_products") List<EarningsOverviewTopProductResponse> topProducts,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
