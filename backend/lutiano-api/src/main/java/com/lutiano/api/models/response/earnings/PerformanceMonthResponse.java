package com.lutiano.api.models.response.earnings;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record PerformanceMonthResponse(
    @JsonProperty("avg_progress_pct") BigDecimal avgProgressPct,
    String month,
    @JsonProperty("monthly_achieved_xaf") Long monthlyAchievedXaf,
    @JsonProperty("monthly_target_xaf") Long monthlyTargetXaf,
    Long reached,
    @JsonProperty("sales_xaf") Long salesXaf,
    Long targets,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
