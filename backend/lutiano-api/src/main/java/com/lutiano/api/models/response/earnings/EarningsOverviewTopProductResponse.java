package com.lutiano.api.models.response.earnings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record EarningsOverviewTopProductResponse(@JsonProperty("commission_xaf") Long commissionXaf, @JsonProperty("product_name") String productName, Long units, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
