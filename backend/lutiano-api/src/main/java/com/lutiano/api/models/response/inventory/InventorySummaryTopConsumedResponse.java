package com.lutiano.api.models.response.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record InventorySummaryTopConsumedResponse(@JsonProperty("product_id") String productId, @JsonProperty("product_name") String productName, Long quantity, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
