package com.lutiano.api.models.response.earnings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record EarningsOperationResponse(
    @JsonProperty("commission_xaf") Long commissionXaf,
    @JsonProperty("operation_id") String operationId,
    @JsonProperty("operation_name") String operationName,
    @JsonProperty("sales_count") Long salesCount,
    @JsonProperty("sales_xaf") Long salesXaf,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
