package com.lutiano.api.models.response.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ReorderResponse(
    @JsonProperty("image_url") String imageUrl,
    Long inbound,
    @JsonProperty("low_stock_threshold") Long lowStockThreshold,
    String name,
    @JsonProperty("operation_code") Object operationCode,
    @JsonProperty("operation_id") Object operationId,
    @JsonProperty("operation_name") String operationName,
    @JsonProperty("product_id") String productId,
    Long quantity,
    String sku,
    @JsonProperty("suggested_quantity") Long suggestedQuantity,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
