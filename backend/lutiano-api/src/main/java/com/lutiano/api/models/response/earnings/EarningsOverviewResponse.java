package com.lutiano.api.models.response.earnings;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record EarningsOverviewResponse(EarningsOverviewPeriodResponse period, List<EarningsOverviewTopEarnerResponse> topEarners, List<EarningsOverviewTopProductResponse> topProducts, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
