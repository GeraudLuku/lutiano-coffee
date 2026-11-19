package com.lutiano.api.models.response.cash;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record OpenResponse(
    @JsonProperty("closed_at") Object closedAt,
    @JsonProperty("counted_cash") Object countedCash,
    @JsonProperty("expected_cash") Long expectedCash,
    String id,
    @JsonProperty("opened_at") String openedAt,
    @JsonProperty("opening_float") Long openingFloat,
    @JsonProperty("refunds_paid_xaf") Long refundsPaidXaf,
    @JsonProperty("review_note") Object reviewNote,
    @JsonProperty("review_status") Object reviewStatus,
    @JsonProperty("reviewed_at") Object reviewedAt,
    @JsonProperty("reviewed_by") Object reviewedBy,
    @JsonProperty("sale_count") Long saleCount,
    @JsonProperty("seller_id") String sellerId,
    @JsonProperty("seller_name") String sellerName,
    String status,
    Object variance,
    @JsonProperty("variance_reason") Object varianceReason,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
