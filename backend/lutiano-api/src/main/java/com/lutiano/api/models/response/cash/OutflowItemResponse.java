package com.lutiano.api.models.response.cash;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record OutflowItemResponse(
    @JsonProperty("amount_xaf") Long amountXaf,
    String category,
    @JsonProperty("decided_at") String decidedAt,
    @JsonProperty("decided_by") String decidedBy,
    @JsonProperty("decided_by_name") String decidedByName,
    @JsonProperty("decision_note") String decisionNote,
    String description,
    String id,
    String reference,
    @JsonProperty("requested_at") String requestedAt,
    @JsonProperty("seller_id") String sellerId,
    @JsonProperty("seller_name") String sellerName,
    @JsonProperty("shift_id") String shiftId,
    String status,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
