package com.lutiano.api.models.response.loyalty;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record LoyaltyRuleResponse(
    @JsonProperty("base_amount_xaf") Long baseAmountXaf,
    @JsonProperty("base_points") Long basePoints,
    @JsonProperty("expiry_months") Long expiryMonths,
    @JsonProperty("min_redeem_points") Long minRedeemPoints,
    List<LoyaltyRuleRuleResponse> rules,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
