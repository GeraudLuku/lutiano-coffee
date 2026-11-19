package com.lutiano.api.models.response.cash;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record LedgerItemResponse(
    @JsonProperty("actor_id") String actorId,
    @JsonProperty("actor_name") String actorName,
    @JsonProperty("amount_xaf") Long amountXaf,
    @JsonProperty("balance_after_xaf") Long balanceAfterXaf,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("entry_type") String entryType,
    String id,
    Object note,
    Long seq,
    @JsonProperty("shift_id") String shiftId,
    @JsonProperty("source_id") String sourceId,
    @JsonProperty("source_reference") String sourceReference,
    @JsonProperty("source_type") String sourceType,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
