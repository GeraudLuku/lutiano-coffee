package com.lutiano.api.models.response.loyalty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record LoyaltyOverviewRedemptionsByTypeResponse(Long points, Long redemptions, @JsonProperty("reward_type") String rewardType, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
