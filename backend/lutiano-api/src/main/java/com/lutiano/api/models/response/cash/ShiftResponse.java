package com.lutiano.api.models.response.cash;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ShiftResponse(
    @JsonProperty("cash_sales_xaf") Long cashSalesXaf,
    @JsonProperty("closed_at") Object closedAt,
    @JsonProperty("count_count") Long countCount,
    @JsonProperty("counted_cash") Object countedCash,
    List<Object> counts,
    @JsonProperty("expected_cash") Long expectedCash,
    String id,
    @JsonProperty("opened_at") String openedAt,
    @JsonProperty("opening_float") Long openingFloat,
    List<Object> outflows,
    @JsonProperty("outflows_xaf") Long outflowsXaf,
    @JsonProperty("outstanding_remittances") Long outstandingRemittances,
    @JsonProperty("pending_outflows") Long pendingOutflows,
    @JsonProperty("refunds_paid_xaf") Long refundsPaidXaf,
    List<Object> remittances,
    @JsonProperty("remitted_xaf") Long remittedXaf,
    @JsonProperty("review_note") Object reviewNote,
    @JsonProperty("review_status") Object reviewStatus,
    @JsonProperty("reviewed_at") Object reviewedAt,
    @JsonProperty("reviewed_by_name") Object reviewedByName,
    @JsonProperty("sale_count") Long saleCount,
    @JsonProperty("seller_id") String sellerId,
    @JsonProperty("seller_name") String sellerName,
    String status,
    Object variance,
    @JsonProperty("variance_reason") Object varianceReason,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
