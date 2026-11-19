package com.lutiano.api.models.response.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record CentralResponse(
    Boolean active,
    String category,
    @JsonProperty("image_url") String imageUrl,
    @JsonProperty("low_stock_threshold") Long lowStockThreshold,
    String name,
    @JsonProperty("product_id") String productId,
    Long quantity,
    String sku,
    String unit,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
