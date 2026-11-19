package com.lutiano.api.models.response.earnings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record RuleItemResponse(
    String basis,
    String category,
    @JsonProperty("created_at") String createdAt,
    String description,
    String id,
    @JsonProperty("product_name") String productName,
    Long rate,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
