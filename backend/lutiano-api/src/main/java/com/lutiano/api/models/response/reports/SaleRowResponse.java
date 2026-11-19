package com.lutiano.api.models.response.reports;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record SaleRowResponse(
    String label,
    @JsonProperty("revenue_xaf") Long revenueXaf,
    @JsonProperty("share_pct") BigDecimal sharePct,
    Long transactions,
    Long units,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
