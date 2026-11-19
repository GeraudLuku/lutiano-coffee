package com.lutiano.api.models.response.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record OrderItemV2Response(
    @JsonProperty("accepted_at") Object acceptedAt,
    @JsonProperty("closed_at") String closedAt,
    @JsonProperty("customer_id") String customerId,
    String id,
    String note,
    @JsonProperty("operation_id") String operationId,
    @JsonProperty("operation_name") String operationName,
    @JsonProperty("payment_method") String paymentMethod,
    @JsonProperty("placed_at") String placedAt,
    @JsonProperty("ready_at") Object readyAt,
    String reason,
    String reference,
    @JsonProperty("sale_id") Object saleId,
    String status,
    @JsonProperty("status_group") String statusGroup,
    @JsonProperty("total_xaf") Long totalXaf,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
