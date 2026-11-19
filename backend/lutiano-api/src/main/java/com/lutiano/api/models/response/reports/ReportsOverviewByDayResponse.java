package com.lutiano.api.models.response.reports;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ReportsOverviewByDayResponse(String date, @JsonProperty("revenue_xaf") Long revenueXaf, Long transactions, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
