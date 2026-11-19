package com.lutiano.api.models.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record AddonResponse(
    @JsonInclude(JsonInclude.Include.NON_NULL) Boolean active,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("created_at") String createdAt,
    String id,
    @JsonProperty("image_url") Object imageUrl,
    String name,
    @JsonProperty("price_xaf") Long priceXaf,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("product_count") Long productCount,
    @JsonInclude(JsonInclude.Include.NON_NULL) List<AddonProductResponse> products,
    @JsonProperty("sort_order") Long sortOrder,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("product_ids") List<String> productIds,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
