package com.lutiano.api.models.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ProductAddonResponse(
    Boolean active,
    @JsonProperty("created_at") String createdAt,
    String id,
    @JsonProperty("image_url") Object imageUrl,
    String name,
    @JsonProperty("price_xaf") Long priceXaf,
    @JsonProperty("sort_order") Long sortOrder,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
