package com.lutiano.api.models.response.loyalty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record SettingResponse(
    @JsonProperty("base_amount_xaf") Long baseAmountXaf,
    @JsonProperty("base_points") Long basePoints,
    @JsonProperty("expiry_months") Long expiryMonths,
    @JsonProperty("expiry_notice_days") Long expiryNoticeDays,
    Long id,
    @JsonProperty("min_redeem_points") Long minRedeemPoints,
    @JsonProperty("point_value_xaf") Long pointValueXaf,
    @JsonProperty("program_active") Boolean programActive,
    @JsonProperty("updated_at") Object updatedAt,
    @JsonProperty("updated_by") Object updatedBy,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
