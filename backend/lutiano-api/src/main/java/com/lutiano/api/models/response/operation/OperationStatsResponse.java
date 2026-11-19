package com.lutiano.api.models.response.operation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record OperationStatsResponse(
    @JsonProperty("month_xaf") Long monthXaf,
    @JsonProperty("today_xaf") Long todayXaf,
    Long transactions,
    @JsonProperty("week_xaf") Long weekXaf,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
