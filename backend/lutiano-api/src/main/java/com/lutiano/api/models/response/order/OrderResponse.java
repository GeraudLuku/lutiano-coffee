package com.lutiano.api.models.response.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record OrderResponse(
    @JsonProperty("accepted_at") Object acceptedAt,
    @JsonProperty("closed_at") String closedAt,
    @JsonProperty("customer_id") String customerId,
    @JsonProperty("customer_name") String customerName,
    @JsonProperty("customer_phone") String customerPhone,
    @JsonProperty("handled_by") Object handledBy,
    String id,
    List<OrderLineResponse> lines,
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
    List<OrderTimelineResponse> timeline,
    @JsonProperty("total_xaf") Long totalXaf,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
