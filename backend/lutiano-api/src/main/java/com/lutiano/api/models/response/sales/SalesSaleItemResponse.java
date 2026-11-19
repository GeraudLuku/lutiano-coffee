package com.lutiano.api.models.response.sales;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record SalesSaleItemResponse(
    @JsonProperty("cash_received_xaf") Long cashReceivedXaf,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("customer_code") String customerCode,
    @JsonProperty("customer_id") String customerId,
    @JsonProperty("customer_name") String customerName,
    String id,
    @JsonProperty("item_count") Long itemCount,
    @JsonProperty("open_refunds") Long openRefunds,
    @JsonProperty("operation_code") String operationCode,
    @JsonProperty("operation_id") String operationId,
    @JsonProperty("operation_name") String operationName,
    @JsonProperty("operation_type") String operationType,
    @JsonProperty("payment_method") String paymentMethod,
    String reference,
    @JsonProperty("refunded_xaf") Long refundedXaf,
    @JsonProperty("seller_id") String sellerId,
    @JsonProperty("seller_name") String sellerName,
    @JsonProperty("shift_id") String shiftId,
    @JsonProperty("total_xaf") Long totalXaf,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
