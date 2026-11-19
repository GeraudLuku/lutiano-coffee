package com.lutiano.api.models.response.earnings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record RuleResponse(
    String basis,
    String category,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("created_by") String createdBy,
    @JsonProperty("created_by_name") String createdByName,
    String id,
    String note,
    @JsonProperty("product_id") String productId,
    @JsonProperty("product_kind") String productKind,
    @JsonProperty("product_name") String productName,
    Long rate,
    @JsonProperty("seller_role") String sellerRole,
    String sku,
    @JsonProperty("superseded_at") Object supersededAt,
    @JsonProperty("superseded_by") Object supersededBy,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
