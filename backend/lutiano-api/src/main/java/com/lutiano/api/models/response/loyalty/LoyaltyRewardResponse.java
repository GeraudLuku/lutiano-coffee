package com.lutiano.api.models.response.loyalty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record LoyaltyRewardResponse(
    Boolean active,
    @JsonProperty("created_at") String createdAt,
    String description,
    @JsonProperty("discount_pct") Long discountPct,
    String id,
    @JsonProperty("image_url") Object imageUrl,
    @JsonProperty("min_purchases") Long minPurchases,
    @JsonProperty("min_spend_xaf") Long minSpendXaf,
    String name,
    @JsonProperty("point_cost") Long pointCost,
    @JsonProperty("product_id") String productId,
    @JsonProperty("product_name") String productName,
    Long redemptions,
    @JsonProperty("requires_stock") Boolean requiresStock,
    @JsonProperty("reward_type") String rewardType,
    @JsonProperty("sort_order") Long sortOrder,
    @JsonProperty("updated_at") Object updatedAt,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
