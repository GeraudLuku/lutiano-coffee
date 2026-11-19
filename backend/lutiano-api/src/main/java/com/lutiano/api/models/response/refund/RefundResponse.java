package com.lutiano.api.models.response.refund;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record RefundResponse(
    @JsonProperty("advance_amount_xaf") Object advanceAmountXaf,
    @JsonProperty("advance_reference") Object advanceReference,
    @JsonProperty("advance_returned_xaf") Object advanceReturnedXaf,
    @JsonProperty("amount_xaf") Long amountXaf,
    @JsonProperty("customer_name") Object customerName,
    @JsonProperty("customer_phone") Object customerPhone,
    @JsonProperty("decided_at") Object decidedAt,
    @JsonProperty("decided_by") Object decidedBy,
    @JsonProperty("decided_by_name") Object decidedByName,
    @JsonProperty("decision_note") Object decisionNote,
    List<RefundEventResponse> events,
    List<Object> evidence,
    @JsonProperty("fund_source") Object fundSource,
    String id,
    String note,
    @JsonProperty("operation_code") String operationCode,
    @JsonProperty("operation_id") String operationId,
    @JsonProperty("operation_name") String operationName,
    @JsonProperty("paid_at") Object paidAt,
    @JsonProperty("paid_by") Object paidBy,
    @JsonProperty("paid_by_name") Object paidByName,
    @JsonProperty("payment_note") Object paymentNote,
    String reason,
    @JsonProperty("recipient_acknowledged") Boolean recipientAcknowledged,
    @JsonProperty("recipient_name") Object recipientName,
    String reference,
    @JsonProperty("refund_type") String refundType,
    @JsonProperty("requested_at") String requestedAt,
    @JsonProperty("requested_by") String requestedBy,
    @JsonProperty("requested_by_name") String requestedByName,
    List<RefundReturnableResponse> returnable,
    @JsonProperty("sale_created_at") String saleCreatedAt,
    @JsonProperty("sale_id") String saleId,
    @JsonProperty("sale_reference") String saleReference,
    @JsonProperty("sale_refundable_xaf") Long saleRefundableXaf,
    @JsonProperty("sale_total_xaf") Long saleTotalXaf,
    @JsonProperty("seller_id") String sellerId,
    @JsonProperty("seller_name") String sellerName,
    @JsonProperty("shift_id") Object shiftId,
    @JsonProperty("shift_seller_id") Object shiftSellerId,
    @JsonProperty("shift_seller_name") Object shiftSellerName,
    String status,
    @JsonProperty("stock_returns") List<Object> stockReturns,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
