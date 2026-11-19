package com.lutiano.api.models.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ProductStockResponse(
    String code,
    @JsonProperty("operation_id") String operationId,
    @JsonProperty("operation_name") String operationName,
    @JsonProperty("product_id") String productId,
    @JsonProperty("product_name") String productName,
    Long quantity,
    @JsonProperty("variant_name") String variantName,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
