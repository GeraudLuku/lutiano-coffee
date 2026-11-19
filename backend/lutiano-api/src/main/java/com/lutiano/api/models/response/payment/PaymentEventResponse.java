package com.lutiano.api.models.response.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record PaymentEventResponse(
    @JsonProperty("actor_id") String actorId,
    @JsonProperty("actor_name") String actorName,
    @JsonProperty("created_at") String createdAt,
    String details,
    @JsonProperty("event_type") String eventType,
    @JsonProperty("from_status") Object fromStatus,
    String id,
    @JsonProperty("payment_id") String paymentId,
    String source,
    @JsonProperty("to_status") String toStatus,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
