package com.lutiano.api.models.response.loyalty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record LoyaltyRuleRuleResponse(
    @JsonProperty("amount_xaf") Long amountXaf,
    String category,
    @JsonProperty("ends_on") Object endsOn,
    String method,
    @JsonProperty("multiplier_pct") Object multiplierPct,
    String name,
    Long points,
    @JsonProperty("product_name") Object productName,
    String scope,
    @JsonProperty("starts_on") Object startsOn,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
