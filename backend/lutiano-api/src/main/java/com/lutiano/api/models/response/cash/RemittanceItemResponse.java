package com.lutiano.api.models.response.cash;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record RemittanceItemResponse(
    @JsonProperty("acknowledged_at") String acknowledgedAt,
    @JsonProperty("acknowledged_by") String acknowledgedBy,
    @JsonProperty("acknowledged_by_name") String acknowledgedByName,
    @JsonProperty("acknowledgement_note") String acknowledgementNote,
    @JsonProperty("amount_xaf") Long amountXaf,
    @JsonProperty("bank_reference") String bankReference,
    String id,
    String method,
    Object note,
    @JsonProperty("received_xaf") Long receivedXaf,
    @JsonProperty("recipient_id") String recipientId,
    @JsonProperty("recipient_name") String recipientName,
    @JsonProperty("recorded_at") String recordedAt,
    String reference,
    @JsonProperty("seller_id") String sellerId,
    @JsonProperty("seller_name") String sellerName,
    @JsonProperty("shift_id") String shiftId,
    String status,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
