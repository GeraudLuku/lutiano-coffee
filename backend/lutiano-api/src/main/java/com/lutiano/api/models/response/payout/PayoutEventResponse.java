package com.lutiano.api.models.response.payout;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record PayoutEventResponse(
    @JsonProperty("actor_id") String actorId,
    @JsonProperty("actor_name") String actorName,
    @JsonProperty("created_at") String createdAt,
    String details,
    @JsonProperty("event_type") String eventType,
    @JsonProperty("from_status") Object fromStatus,
    String id,
    @JsonProperty("payout_id") String payoutId,
    @JsonProperty("to_status") String toStatus,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
