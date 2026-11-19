package com.lutiano.api.models.response.earnings;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record PerformanceResponse(
    @JsonProperty("bonus_xaf") Long bonusXaf,
    List<PerformanceMonthResponse> months,
    Long reached,
    Long targets,
    @JsonProperty("total_sales_xaf") Long totalSalesXaf,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
