package com.lutiano.api.models.response.staff;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record StaffPerformanceByOperationResponse(
    @JsonProperty("operation_id") String operationId,
    @JsonProperty("operation_name") String operationName,
    @JsonProperty("operation_type") String operationType,
    @JsonProperty("revenue_xaf") Long revenueXaf,
    Long sales,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
