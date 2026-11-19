package com.lutiano.api.models.response.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record MovementItemResponse(
    @JsonProperty("balance_after") Long balanceAfter,
    String direction,
    @JsonProperty("movement_type") String movementType,
    @JsonProperty("occurred_at") String occurredAt,
    @JsonProperty("product_id") String productId,
    @JsonProperty("product_name") String productName,
    Long quantity,
    String reference,
    String source,
    @JsonProperty("source_id") String sourceId,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
