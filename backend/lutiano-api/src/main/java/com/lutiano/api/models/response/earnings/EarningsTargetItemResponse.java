package com.lutiano.api.models.response.earnings;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record EarningsTargetItemResponse(
    Long achieved,
    @JsonProperty("bonus_earned") Long bonusEarned,
    @JsonProperty("bonus_month") String bonusMonth,
    @JsonProperty("bonus_paid_xaf") Long bonusPaidXaf,
    @JsonProperty("bonus_reversed") Boolean bonusReversed,
    @JsonProperty("bonus_state") String bonusState,
    @JsonProperty("bonus_type") String bonusType,
    @JsonProperty("bonus_xaf") Long bonusXaf,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("ends_on") String endsOn,
    String id,
    String metric,
    String name,
    @JsonProperty("period_type") String periodType,
    @JsonProperty("product_id") String productId,
    @JsonProperty("product_name") String productName,
    @JsonProperty("progress_pct") BigDecimal progressPct,
    Boolean reached,
    @JsonProperty("seller_id") String sellerId,
    @JsonProperty("starts_on") String startsOn,
    String state,
    String status,
    @JsonProperty("target_value") Long targetValue,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
