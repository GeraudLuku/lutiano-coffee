package com.lutiano.api.models.response.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record SupportItemResponse(
    @JsonProperty("attachment_name") String attachmentName,
    @JsonProperty("attachment_type") String attachmentType,
    @JsonProperty("created_at") String createdAt,
    String description,
    @JsonProperty("has_attachment") Long hasAttachment,
    String id,
    String reference,
    @JsonProperty("replied_at") Object repliedAt,
    @JsonProperty("replied_by_name") Object repliedByName,
    Object reply,
    @JsonProperty("requester_type") String requesterType,
    String status,
    String subject,
    String type,
    @JsonProperty("user_id") String userId,
    @JsonProperty("user_name") String userName,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
