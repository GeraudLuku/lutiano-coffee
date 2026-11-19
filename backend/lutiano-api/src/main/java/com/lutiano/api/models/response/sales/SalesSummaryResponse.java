package com.lutiano.api.models.response.sales;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record SalesSummaryResponse(SalesSummaryMonthResponse month, SalesSummaryMonthResponse today, List<SalesSummaryWeekResponse> week, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
