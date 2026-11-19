package com.lutiano.api.models.response.loyalty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record CustomerResponse(
    String code,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("expiring_soon") Long expiringSoon,
    @JsonProperty("expiry_notice_days") Long expiryNoticeDays,
    @JsonProperty("full_name") String fullName,
    String id,
    @JsonProperty("last_purchase_at") Object lastPurchaseAt,
    @JsonProperty("lifetime_spend_xaf") Long lifetimeSpendXaf,
    @JsonProperty("loyalty_token") String loyaltyToken,
    String phone,
    @JsonProperty("phone_verified") Boolean phoneVerified,
    @JsonProperty("points_balance") Long pointsBalance,
    Long purchases,
    @JsonProperty("rewards_redeemed") Long rewardsRedeemed,
    String status,
    @JsonProperty("tier_code") String tierCode,
    @JsonProperty("tier_color") String tierColor,
    @JsonProperty("tier_name") String tierName,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
