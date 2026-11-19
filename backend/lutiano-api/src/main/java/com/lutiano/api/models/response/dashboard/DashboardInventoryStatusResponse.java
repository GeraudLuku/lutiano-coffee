package com.lutiano.api.models.response.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record DashboardInventoryStatusResponse(
    @JsonProperty("image_url") String imageUrl,
    @JsonProperty("low_stock_threshold") Long lowStockThreshold,
    String name,
    @JsonProperty("product_id") String productId,
    Long quantity,
    String sku,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
