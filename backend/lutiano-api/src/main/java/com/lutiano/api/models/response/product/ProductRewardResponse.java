package com.lutiano.api.models.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ProductRewardResponse(
    List<ProductRewardCommissionResponse> commission,
    @JsonProperty("loyalty_eligible") Boolean loyaltyEligible,
    @JsonProperty("loyalty_points") Long loyaltyPoints,
    String name,
    @JsonProperty("price_xaf") Long priceXaf,
    @JsonProperty("product_id") String productId,
    String sku,
    @JsonProperty("variant_name") String variantName,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
