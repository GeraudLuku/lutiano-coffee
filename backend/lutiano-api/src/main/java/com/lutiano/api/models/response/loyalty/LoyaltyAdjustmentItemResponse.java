package com.lutiano.api.models.response.loyalty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record LoyaltyAdjustmentItemResponse(
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("created_by") String createdBy,
    @JsonProperty("created_by_name") String createdByName,
    @JsonProperty("customer_code") String customerCode,
    @JsonProperty("customer_id") String customerId,
    @JsonProperty("customer_name") String customerName,
    String id,
    String note,
    Long points,
    String reason,
    String reference,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
