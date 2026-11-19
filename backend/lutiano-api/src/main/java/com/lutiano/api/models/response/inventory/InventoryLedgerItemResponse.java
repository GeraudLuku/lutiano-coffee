package com.lutiano.api.models.response.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record InventoryLedgerItemResponse(
    @JsonProperty("actor_id") String actorId,
    @JsonProperty("actor_name") String actorName,
    @JsonProperty("balance_after") Long balanceAfter,
    @JsonProperty("created_at") String createdAt,
    String id,
    @JsonProperty("location_code") String locationCode,
    @JsonProperty("location_name") String locationName,
    @JsonProperty("movement_type") String movementType,
    Object note,
    @JsonProperty("operation_id") String operationId,
    @JsonProperty("product_id") String productId,
    @JsonProperty("product_name") String productName,
    @JsonProperty("quantity_delta") Long quantityDelta,
    @JsonProperty("reference_id") String referenceId,
    @JsonProperty("reference_label") String referenceLabel,
    @JsonProperty("reference_type") String referenceType,
    String sku,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
