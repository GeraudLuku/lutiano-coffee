package com.lutiano.api.models.response.loyalty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record RedemptionItemResponse(
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("created_by") String createdBy,
    @JsonProperty("created_by_name") String createdByName,
    @JsonProperty("customer_code") String customerCode,
    @JsonProperty("customer_id") String customerId,
    @JsonProperty("customer_name") String customerName,
    String id,
    @JsonProperty("operation_id") String operationId,
    @JsonProperty("operation_name") String operationName,
    Long points,
    String reference,
    @JsonProperty("reversal_reason") Object reversalReason,
    @JsonProperty("reversed_at") Object reversedAt,
    @JsonProperty("reversed_by") Object reversedBy,
    @JsonProperty("reversed_by_name") Object reversedByName,
    @JsonProperty("reward_id") String rewardId,
    @JsonProperty("reward_name") String rewardName,
    @JsonProperty("reward_type") String rewardType,
    String status,
    String token,
    @JsonProperty("use_state") String useState,
    @JsonProperty("used_at") String usedAt,
    @JsonProperty("used_by") String usedBy,
    @JsonProperty("used_operation_id") String usedOperationId,
    @JsonProperty("valid_until") String validUntil,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
