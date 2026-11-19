package com.lutiano.api.models.response.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record NotificationMineItemResponse(
    String body,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("event_key") String eventKey,
    @JsonProperty("event_name") String eventName,
    String id,
    @JsonProperty("read_at") String readAt,
    @JsonProperty("related_id") String relatedId,
    @JsonProperty("related_type") String relatedType,
    String topic,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
