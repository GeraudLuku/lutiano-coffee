package com.lutiano.api.models.response.cash;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ShiftItemResponse(
    @JsonProperty("cash_sales_xaf") Long cashSalesXaf,
    @JsonProperty("closed_at") String closedAt,
    @JsonProperty("count_count") Long countCount,
    @JsonProperty("counted_cash") Long countedCash,
    @JsonProperty("expected_cash") Long expectedCash,
    String id,
    @JsonProperty("opened_at") String openedAt,
    @JsonProperty("opening_float") Long openingFloat,
    @JsonProperty("outflows_xaf") Long outflowsXaf,
    @JsonProperty("outstanding_remittances") Long outstandingRemittances,
    @JsonProperty("pending_outflows") Long pendingOutflows,
    @JsonProperty("refunds_paid_xaf") Long refundsPaidXaf,
    @JsonProperty("remitted_xaf") Long remittedXaf,
    @JsonProperty("review_note") Object reviewNote,
    @JsonProperty("review_status") String reviewStatus,
    @JsonProperty("reviewed_at") Object reviewedAt,
    @JsonProperty("reviewed_by_name") Object reviewedByName,
    @JsonProperty("sale_count") Long saleCount,
    @JsonProperty("seller_id") String sellerId,
    @JsonProperty("seller_name") String sellerName,
    String status,
    Long variance,
    @JsonProperty("variance_reason") Object varianceReason,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
