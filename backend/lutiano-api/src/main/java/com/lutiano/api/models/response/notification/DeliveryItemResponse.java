package com.lutiano.api.models.response.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record DeliveryItemResponse(
    Long attempts,
    String body,
    @JsonProperty("created_at") String createdAt,
    String error,
    @JsonProperty("event_key") String eventKey,
    @JsonProperty("event_name") String eventName,
    String id,
    String language,
    @JsonProperty("phone_masked") String phoneMasked,
    @JsonProperty("read_at") String readAt,
    @JsonProperty("recipient_id") String recipientId,
    @JsonProperty("recipient_name") String recipientName,
    @JsonProperty("related_id") String relatedId,
    @JsonProperty("related_type") String relatedType,
    Boolean resendable,
    @JsonProperty("sent_at") String sentAt,
    String status,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
