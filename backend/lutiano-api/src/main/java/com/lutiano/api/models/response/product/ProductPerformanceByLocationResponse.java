package com.lutiano.api.models.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ProductPerformanceByLocationResponse(
    @JsonProperty("operation_id") String operationId,
    @JsonProperty("operation_name") String operationName,
    @JsonProperty("revenue_xaf") Long revenueXaf,
    Long units,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
