package com.lutiano.api.models.response.earnings;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record SellerResponse(EarningsOverviewTopEarnerResponse current, EarningsOverviewPeriodResponse period, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
