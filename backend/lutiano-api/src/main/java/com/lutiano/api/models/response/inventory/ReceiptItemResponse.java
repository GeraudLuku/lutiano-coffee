package com.lutiano.api.models.response.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ReceiptItemResponse(
    @JsonProperty("actor_id") String actorId,
    @JsonProperty("actor_name") String actorName,
    @JsonProperty("created_at") String createdAt,
    String id,
    Object note,
    @JsonProperty("product_id") String productId,
    @JsonProperty("product_name") String productName,
    Long quantity,
    String reference,
    String sku,
    String source,
    String supplier,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
