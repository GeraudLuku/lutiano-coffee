package com.lutiano.api.models.response.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record AnnouncementItemResponse(
    String audience,
    String body,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("created_by") String createdBy,
    @JsonProperty("created_by_name") String createdByName,
    String id,
    Boolean important,
    @JsonProperty("operation_id") Object operationId,
    @JsonProperty("operation_name") Object operationName,
    @JsonProperty("recipient_count") Long recipientCount,
    @JsonProperty("seller_role") String sellerRole,
    String title,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
