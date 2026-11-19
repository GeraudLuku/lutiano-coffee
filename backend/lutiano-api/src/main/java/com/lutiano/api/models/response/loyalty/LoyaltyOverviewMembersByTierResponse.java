package com.lutiano.api.models.response.loyalty;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record LoyaltyOverviewMembersByTierResponse(
    String code,
    String color,
    Long members,
    String name,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
