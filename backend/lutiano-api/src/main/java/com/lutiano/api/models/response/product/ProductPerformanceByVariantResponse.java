package com.lutiano.api.models.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ProductPerformanceByVariantResponse(
    String name,
    @JsonProperty("product_id") String productId,
    @JsonProperty("revenue_xaf") Long revenueXaf,
    Long units,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
