package com.lutiano.api.models.response.earnings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record AdjustmentItemResponse(
    @JsonProperty("adjustment_type") String adjustmentType,
    @JsonProperty("amount_xaf") Long amountXaf,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("created_by") String createdBy,
    @JsonProperty("created_by_name") String createdByName,
    String id,
    String note,
    String reference,
    @JsonProperty("seller_id") String sellerId,
    @JsonProperty("seller_name") String sellerName,
    @JsonProperty("source_id") String sourceId,
    @JsonProperty("source_type") String sourceType,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
