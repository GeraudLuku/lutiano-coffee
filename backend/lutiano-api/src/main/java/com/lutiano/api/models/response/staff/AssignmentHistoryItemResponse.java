package com.lutiano.api.models.response.staff;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record AssignmentHistoryItemResponse(
    String action,
    @JsonProperty("actor_id") String actorId,
    @JsonProperty("actor_name") String actorName,
    @JsonProperty("created_at") String createdAt,
    String id,
    Object note,
    @JsonProperty("operation_code") String operationCode,
    @JsonProperty("operation_id") String operationId,
    @JsonProperty("operation_name") String operationName,
    @JsonProperty("operation_type") String operationType,
    @JsonProperty("seller_id") String sellerId,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
