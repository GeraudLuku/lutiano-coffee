package com.lutiano.api.models.response.reports;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ReportsOverviewResponse(
    @JsonProperty("avg_commission_pct") BigDecimal avgCommissionPct,
    List<ReportsOverviewByChannelResponse> byChannel,
    List<ReportsOverviewByDayResponse> byDay,
    ReportsOverviewCurrentResponse current,
    Long days,
    String from,
    ReportsOverviewCurrentResponse previous,
    @JsonProperty("refunded_xaf") Long refundedXaf,
    String to,
    ReportsOverviewByChannelResponse topProduct,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
