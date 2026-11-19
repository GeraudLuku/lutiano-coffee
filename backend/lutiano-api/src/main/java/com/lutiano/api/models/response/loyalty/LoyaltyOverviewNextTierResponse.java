package com.lutiano.api.models.response.loyalty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record LoyaltyOverviewNextTierResponse(
    String benefits,
    String code,
    String color,
    @JsonProperty("min_annual_spend_xaf") Long minAnnualSpendXaf,
    @JsonProperty("multiplier_pct") Long multiplierPct,
    String name,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
