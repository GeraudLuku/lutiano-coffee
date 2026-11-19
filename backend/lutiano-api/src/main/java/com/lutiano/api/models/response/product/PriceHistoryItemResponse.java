package com.lutiano.api.models.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record PriceHistoryItemResponse(
    @JsonProperty("changed_at") String changedAt,
    @JsonProperty("changed_by") String changedBy,
    @JsonProperty("changed_by_name") String changedByName,
    String id,
    @JsonProperty("previous_price_xaf") Object previousPriceXaf,
    @JsonProperty("price_xaf") Long priceXaf,
    @JsonProperty("product_id") String productId,
    @JsonProperty("product_name") String productName,
    @JsonProperty("variant_name") String variantName,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
