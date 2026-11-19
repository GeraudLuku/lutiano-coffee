package com.lutiano.api.models.response.catalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record CatalogueProductItemResponse(
    List<ProductItemAddonResponse> addons,
    Boolean available,
    String category,
    String description,
    String id,
    @JsonProperty("image_url") Object imageUrl,
    String kind,
    String name,
    @JsonProperty("price_xaf") Long priceXaf,
    List<ProductItemVariantResponse> variants,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
