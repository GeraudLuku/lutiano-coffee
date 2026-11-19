package com.lutiano.api.models.response.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record NotificationItemResponse(
    String body,
    String category,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("event_key") String eventKey,
    String id,
    Boolean important,
    @JsonProperty("read_at") Object readAt,
    @JsonProperty("related_id") String relatedId,
    @JsonProperty("related_type") String relatedType,
    String title,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
