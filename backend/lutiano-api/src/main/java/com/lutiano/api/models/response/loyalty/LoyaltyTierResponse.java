package com.lutiano.api.models.response.loyalty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record LoyaltyTierResponse(
    Boolean active,
    String benefits,
    String code,
    String color,
    Long members,
    @JsonProperty("min_annual_spend_xaf") Long minAnnualSpendXaf,
    @JsonProperty("multiplier_pct") Long multiplierPct,
    String name,
    @JsonProperty("sort_order") Long sortOrder,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
