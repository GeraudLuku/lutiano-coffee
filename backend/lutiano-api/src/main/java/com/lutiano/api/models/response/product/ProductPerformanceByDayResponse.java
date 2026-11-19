package com.lutiano.api.models.response.product;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ProductPerformanceByDayResponse(String date, Long revenueXaf, Long units, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
