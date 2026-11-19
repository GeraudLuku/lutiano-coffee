package com.lutiano.api.models.response.dashboard;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record DashboardSalesByDayResponse(String date, Long revenueXaf, Long sales, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
