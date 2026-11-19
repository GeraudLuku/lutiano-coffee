package com.lutiano.api.models.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ProductStatsResponse(
    @JsonProperty("commission_xaf") Long commissionXaf,
    @JsonProperty("points_awarded") Long pointsAwarded,
    @JsonProperty("revenue_xaf") Long revenueXaf,
    Long transactions,
    @JsonProperty("units_sold") Long unitsSold,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
