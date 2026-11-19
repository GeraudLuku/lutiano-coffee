package com.lutiano.api.models.response.loyalty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record LoyaltyOverviewResponse(
    @JsonProperty("annual_spend_xaf") Long annualSpendXaf,
    @JsonProperty("expiring_points") Long expiringPoints,
    @JsonProperty("expiry_notice_days") Long expiryNoticeDays,
    @JsonProperty("min_redeem_points") Long minRedeemPoints,
    @JsonProperty("next_tier") LoyaltyOverviewNextTierResponse nextTier,
    @JsonProperty("points_balance") Long pointsBalance,
    @JsonProperty("programme_active") Boolean programmeActive,
    @JsonProperty("remaining_to_next_xaf") Long remainingToNextXaf,
    @JsonProperty("rewards_available") Long rewardsAvailable,
    LoyaltyOverviewNextTierResponse tier,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
