package com.lutiano.api.models.response.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ApprovalRequestsToApproveResponse(
    @JsonProperty("approved_at") Object approvedAt,
    @JsonProperty("approved_by") Object approvedBy,
    @JsonProperty("approved_by_name") Object approvedByName,
    @JsonProperty("closed_at") Object closedAt,
    @JsonProperty("closed_by") Object closedBy,
    @JsonProperty("closed_by_name") Object closedByName,
    @JsonProperty("completed_at") Object completedAt,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("decision_note") Object decisionNote,
    @JsonProperty("dispatched_at") Object dispatchedAt,
    String id,
    @JsonProperty("idempotency_key") String idempotencyKey,
    @JsonProperty("item_count") Long itemCount,
    List<ApprovalRequestsToApproveItemResponse> items,
    @JsonProperty("needed_by") Object neededBy,
    @JsonProperty("operation_code") String operationCode,
    @JsonProperty("operation_id") String operationId,
    @JsonProperty("operation_name") String operationName,
    Object reason,
    String reference,
    @JsonProperty("requested_by") String requestedBy,
    @JsonProperty("requested_by_name") String requestedByName,
    String status,
    @JsonProperty("total_quantity") Long totalQuantity,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
