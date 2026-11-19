package com.lutiano.api.models.response.sales;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record SalesSummaryMonthResponse(
    @JsonProperty("commission_xaf") Long commissionXaf,
    Long customers,
    Long orders,
    @JsonProperty("points_issued") Long pointsIssued,
    @JsonProperty("sales_xaf") Long salesXaf,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
