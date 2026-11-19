package com.lutiano.api.models.response.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record DashboardTopProductResponse(
    @JsonProperty("product_id") String productId,
    @JsonProperty("product_name") String productName,
    @JsonProperty("revenue_xaf") Long revenueXaf,
    Long units,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
