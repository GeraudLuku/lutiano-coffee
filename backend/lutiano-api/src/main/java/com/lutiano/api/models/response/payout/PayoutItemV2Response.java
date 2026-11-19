package com.lutiano.api.models.response.payout;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record PayoutItemV2Response(
    @JsonProperty("amount_xaf") Long amountXaf,
    @JsonProperty("decided_at") String decidedAt,
    @JsonProperty("decided_by") String decidedBy,
    @JsonProperty("decided_by_name") String decidedByName,
    @JsonProperty("decision_note") Object decisionNote,
    @JsonProperty("destination_id") String destinationId,
    @JsonProperty("destination_name") String destinationName,
    @JsonProperty("destination_phone") String destinationPhone,
    String id,
    String method,
    Object note,
    @JsonProperty("paid_at") String paidAt,
    @JsonProperty("paid_by") String paidBy,
    @JsonProperty("paid_by_name") String paidByName,
    @JsonProperty("payment_note") Object paymentNote,
    @JsonProperty("period_ends_on") String periodEndsOn,
    @JsonProperty("period_id") String periodId,
    @JsonProperty("period_starts_on") String periodStartsOn,
    @JsonProperty("provider_reference") Object providerReference,
    String reference,
    @JsonProperty("requested_at") String requestedAt,
    @JsonProperty("requested_by") String requestedBy,
    @JsonProperty("requested_by_name") String requestedByName,
    @JsonProperty("requires_approval") Boolean requiresApproval,
    @JsonProperty("reversal_reason") Object reversalReason,
    @JsonProperty("reversal_requested_at") Object reversalRequestedAt,
    @JsonProperty("reversal_requested_by") Object reversalRequestedBy,
    @JsonProperty("reversal_requested_by_name") Object reversalRequestedByName,
    @JsonProperty("reversed_at") Object reversedAt,
    @JsonProperty("reversed_by") Object reversedBy,
    @JsonProperty("reversed_by_name") Object reversedByName,
    @JsonProperty("seller_id") String sellerId,
    @JsonProperty("seller_name") String sellerName,
    @JsonProperty("seller_role") String sellerRole,
    String status,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
