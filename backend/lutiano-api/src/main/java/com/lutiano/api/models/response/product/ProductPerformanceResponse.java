package com.lutiano.api.models.response.product;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ProductPerformanceResponse(
    List<ProductPerformanceByDayResponse> byDay,
    List<ProductPerformanceByLocationResponse> byLocation,
    List<ProductPerformanceByVariantResponse> byVariant,
    Long days,
    ProductPerformanceTotalsResponse totals,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
