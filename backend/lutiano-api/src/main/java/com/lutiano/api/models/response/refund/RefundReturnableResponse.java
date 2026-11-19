package com.lutiano.api.models.response.refund;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record RefundReturnableResponse(
    @JsonProperty("product_id") String productId,
    @JsonProperty("product_name") String productName,
    Long returnable,
    Long returned,
    Long sold,
    Boolean tracked,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
