package com.lutiano.api.models.response.reports;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ReportsOverviewCurrentResponse(
    @JsonProperty("commission_xaf") Long commissionXaf,
    @JsonProperty("revenue_xaf") Long revenueXaf,
    Long sellers,
    Long transactions,
    Long units,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
