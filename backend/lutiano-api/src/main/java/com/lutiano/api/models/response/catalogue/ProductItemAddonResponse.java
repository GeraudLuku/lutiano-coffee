package com.lutiano.api.models.response.catalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ProductItemAddonResponse(
    String id,
    @JsonProperty("image_url") Object imageUrl,
    String name,
    @JsonProperty("price_xaf") Long priceXaf,
    @JsonProperty("product_id") String productId,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
