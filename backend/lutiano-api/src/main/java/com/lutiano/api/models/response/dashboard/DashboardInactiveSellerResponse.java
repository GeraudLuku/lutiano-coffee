package com.lutiano.api.models.response.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record DashboardInactiveSellerResponse(
    @JsonProperty("active_operations") Long activeOperations,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("last_login_at") String lastLoginAt,
    @JsonProperty("last_sale_at") String lastSaleAt,
    @JsonProperty("seller_id") String sellerId,
    @JsonProperty("seller_name") String sellerName,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
