package com.lutiano.api.models.response.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ApprovalRequestsToApproveItemResponse(
    @JsonProperty("approved_at") Object approvedAt,
    @JsonProperty("approved_by") Object approvedBy,
    @JsonProperty("approved_by_name") Object approvedByName,
    @JsonProperty("central_quantity") Long centralQuantity,
    @JsonProperty("closed_at") Object closedAt,
    @JsonProperty("closed_by") Object closedBy,
    @JsonProperty("closed_by_name") Object closedByName,
    @JsonProperty("confirmed_by") Object confirmedBy,
    @JsonProperty("confirmed_by_name") Object confirmedByName,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("created_by") String createdBy,
    @JsonProperty("created_by_name") String createdByName,
    @JsonProperty("created_by_role") String createdByRole,
    @JsonProperty("decision_note") Object decisionNote,
    @JsonProperty("dispatched_at") Object dispatchedAt,
    @JsonProperty("dispatched_by") Object dispatchedBy,
    @JsonProperty("dispatched_by_name") Object dispatchedByName,
    String id,
    @JsonProperty("image_url") Object imageUrl,
    @JsonProperty("operation_code") String operationCode,
    @JsonProperty("operation_id") String operationId,
    @JsonProperty("operation_name") String operationName,
    @JsonProperty("product_id") String productId,
    @JsonProperty("product_name") String productName,
    @JsonProperty("received_at") Object receivedAt,
    @JsonProperty("received_quantity") Object receivedQuantity,
    @JsonProperty("request_id") String requestId,
    @JsonProperty("request_note") Object requestNote,
    @JsonProperty("sent_quantity") Long sentQuantity,
    String sku,
    String status,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
