package com.lutiano.api.models.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ProductCategoryResponse(
    Boolean active,
    @JsonProperty("created_at") String createdAt,
    String description,
    String id,
    @JsonProperty("image_url") Object imageUrl,
    String name,
    @JsonProperty("product_count") Long productCount,
    @JsonProperty("sort_order") Long sortOrder,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
