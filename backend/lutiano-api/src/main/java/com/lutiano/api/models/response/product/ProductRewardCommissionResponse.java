package com.lutiano.api.models.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ProductRewardCommissionResponse(String basis, Long rate, @JsonProperty("seller_role") String sellerRole, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
