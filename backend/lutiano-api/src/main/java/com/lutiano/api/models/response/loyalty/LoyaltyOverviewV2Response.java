package com.lutiano.api.models.response.loyalty;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record LoyaltyOverviewV2Response(
    @JsonProperty("active_members") Long activeMembers,
    List<LoyaltyOverviewDailyResponse> daily,
    String from,
    Long members,
    List<LoyaltyOverviewMembersByTierResponse> membersByTier,
    @JsonProperty("point_value_xaf") Long pointValueXaf,
    @JsonProperty("points_balance") Long pointsBalance,
    @JsonProperty("points_earned") Long pointsEarned,
    @JsonProperty("points_expired") Long pointsExpired,
    @JsonProperty("points_redeemed") Long pointsRedeemed,
    Long redemptions,
    List<LoyaltyOverviewRedemptionsByTypeResponse> redemptionsByType,
    String to,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
