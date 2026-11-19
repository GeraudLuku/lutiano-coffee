package com.lutiano.api.models.response.reports;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record SaleResponse(
    @JsonProperty("avg_order_xaf") Long avgOrderXaf,
    List<ReportsOverviewByDayResponse> byDay,
    String from,
    String groupBy,
    ReportsOverviewCurrentResponse previous,
    List<SaleRowResponse> rows,
    String to,
    ReportsOverviewCurrentResponse totals,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
