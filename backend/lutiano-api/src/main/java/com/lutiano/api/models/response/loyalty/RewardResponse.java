package com.lutiano.api.models.response.loyalty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record RewardResponse(
    @JsonProperty("blocked_by") String blockedBy,
    @JsonProperty("can_redeem") Boolean canRedeem,
    String description,
    @JsonProperty("discount_pct") Object discountPct,
    String id,
    @JsonProperty("image_url") Object imageUrl,
    @JsonProperty("min_purchases") Long minPurchases,
    @JsonProperty("min_spend_xaf") Long minSpendXaf,
    String name,
    @JsonProperty("point_cost") Long pointCost,
    @JsonProperty("product_name") String productName,
    @JsonProperty("reward_type") String rewardType,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
