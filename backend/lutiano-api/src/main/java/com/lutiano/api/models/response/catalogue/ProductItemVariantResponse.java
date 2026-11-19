package com.lutiano.api.models.response.catalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ProductItemVariantResponse(
    String id,
    @JsonProperty("image_url") Object imageUrl,
    @JsonProperty("parent_id") String parentId,
    @JsonProperty("price_xaf") Long priceXaf,
    @JsonProperty("variant_name") String variantName,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
