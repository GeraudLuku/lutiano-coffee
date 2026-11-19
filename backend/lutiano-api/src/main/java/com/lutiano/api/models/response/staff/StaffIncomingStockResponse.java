package com.lutiano.api.models.response.staff;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record StaffIncomingStockResponse(
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("dispatched_at") String dispatchedAt,
    String id,
    @JsonProperty("operation_name") String operationName,
    @JsonProperty("product_name") String productName,
    @JsonProperty("sent_quantity") Long sentQuantity,
    String status,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
