package com.lutiano.api.models.response.loyalty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record LoyaltyRuleV2Response(
    Boolean active,
    @JsonProperty("amount_xaf") Long amountXaf,
    String category,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("ends_on") String endsOn,
    String id,
    String method,
    @JsonProperty("multiplier_pct") Long multiplierPct,
    String name,
    Long points,
    @JsonProperty("product_id") String productId,
    @JsonProperty("product_name") String productName,
    String scope,
    @JsonProperty("starts_on") String startsOn,
    @JsonProperty("updated_at") Object updatedAt,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
