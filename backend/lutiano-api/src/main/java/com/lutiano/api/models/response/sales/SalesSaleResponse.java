package com.lutiano.api.models.response.sales;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.lutiano.api.models.response.earnings.LineItemResponse;
import com.lutiano.api.models.response.payment.PaymentResponse;
import com.lutiano.api.models.response.refund.RefundItemResponse;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record SalesSaleResponse(
    @JsonProperty("cash_received_xaf") Long cashReceivedXaf,
    @JsonProperty("change_xaf") Long changeXaf,
    List<LineItemResponse> commissions,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("customer_code") Object customerCode,
    @JsonProperty("customer_id") Object customerId,
    @JsonProperty("customer_name") Object customerName,
    String id,
    @JsonProperty("item_count") Long itemCount,
    List<SalesSaleLineResponse> lines,
    Object loyalty,
    @JsonProperty("open_refunds") Long openRefunds,
    @JsonProperty("operation_code") String operationCode,
    @JsonProperty("operation_id") String operationId,
    @JsonProperty("operation_name") String operationName,
    @JsonProperty("operation_type") String operationType,
    @JsonProperty("payment_method") String paymentMethod,
    @JsonProperty("payment_status") String paymentStatus,
    List<PaymentResponse> payments,
    String reference,
    @JsonProperty("refunded_xaf") Long refundedXaf,
    List<RefundItemResponse> refunds,
    @JsonProperty("seller_id") String sellerId,
    @JsonProperty("seller_name") String sellerName,
    @JsonProperty("shift_id") String shiftId,
    String status,
    @JsonProperty("total_xaf") Long totalXaf,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
