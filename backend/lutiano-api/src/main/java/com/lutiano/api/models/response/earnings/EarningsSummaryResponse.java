package com.lutiano.api.models.response.earnings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record EarningsSummaryResponse(
    @JsonProperty("bonus_included") Long bonusIncluded,
    @JsonProperty("bonus_included_xaf") Long bonusIncludedXaf,
    @JsonProperty("bonus_pending") Long bonusPending,
    @JsonProperty("bonus_pending_xaf") Long bonusPendingXaf,
    Long current,
    EarningsSummaryMainResponse main,
    String month,
    Long reached,
    Long targets,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
