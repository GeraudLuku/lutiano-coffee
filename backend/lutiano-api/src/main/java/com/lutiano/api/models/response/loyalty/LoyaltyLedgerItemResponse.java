package com.lutiano.api.models.response.loyalty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record LoyaltyLedgerItemResponse(
    @JsonProperty("actor_id") String actorId,
    @JsonProperty("actor_name") String actorName,
    @JsonProperty("balance_after") Long balanceAfter,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("customer_code") String customerCode,
    @JsonProperty("customer_id") String customerId,
    @JsonProperty("customer_name") String customerName,
    String description,
    @JsonProperty("entry_type") String entryType,
    String id,
    Long points,
    String reference,
    @JsonProperty("source_id") String sourceId,
    @JsonProperty("source_type") String sourceType,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
