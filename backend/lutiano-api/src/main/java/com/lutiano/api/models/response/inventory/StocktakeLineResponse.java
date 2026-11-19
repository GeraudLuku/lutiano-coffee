package com.lutiano.api.models.response.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record StocktakeLineResponse(
    String category,
    @JsonProperty("counted_quantity") Long countedQuantity,
    Long difference,
    @JsonProperty("expected_quantity") Long expectedQuantity,
    @JsonProperty("image_url") String imageUrl,
    @JsonProperty("product_id") String productId,
    @JsonProperty("product_name") String productName,
    String sku,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
