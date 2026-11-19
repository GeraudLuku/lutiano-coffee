package com.lutiano.api.models.response.loyalty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record EstimateResponse(@JsonProperty("amount_xaf") Long amountXaf, Long points, @JsonProperty("tier_multiplier_pct") Long tierMultiplierPct, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
