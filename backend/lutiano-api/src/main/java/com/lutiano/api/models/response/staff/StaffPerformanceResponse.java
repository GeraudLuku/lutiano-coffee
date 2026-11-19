package com.lutiano.api.models.response.staff;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record StaffPerformanceResponse(
    Long items,
    @JsonProperty("last_sale_at") String lastSaleAt,
    @JsonProperty("revenue_xaf") Long revenueXaf,
    Long sales,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
