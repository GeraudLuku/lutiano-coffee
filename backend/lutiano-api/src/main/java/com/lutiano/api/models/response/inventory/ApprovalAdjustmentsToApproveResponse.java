package com.lutiano.api.models.response.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ApprovalAdjustmentsToApproveResponse(
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("decided_at") Object decidedAt,
    @JsonProperty("decided_by") Object decidedBy,
    @JsonProperty("decided_by_name") Object decidedByName,
    @JsonProperty("decision_note") Object decisionNote,
    String id,
    @JsonProperty("location_code") String locationCode,
    @JsonProperty("location_name") String locationName,
    String note,
    @JsonProperty("operation_id") String operationId,
    @JsonProperty("product_id") String productId,
    @JsonProperty("product_name") String productName,
    @JsonProperty("quantity_delta") Long quantityDelta,
    String reason,
    @JsonProperty("refund_id") String refundId,
    @JsonProperty("requested_by") String requestedBy,
    @JsonProperty("requested_by_name") String requestedByName,
    @JsonProperty("requested_by_role") String requestedByRole,
    String sku,
    String status,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
