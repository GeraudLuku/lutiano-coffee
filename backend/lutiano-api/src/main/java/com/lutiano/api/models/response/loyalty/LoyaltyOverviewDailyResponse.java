package com.lutiano.api.models.response.loyalty;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record LoyaltyOverviewDailyResponse(String day, Long earned, Long redeemed, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
