package com.lutiano.api.models.response.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record NotificationSettingResponse(List<NotificationSettingApproverResponse> approvers, String mode, @JsonProperty("sender_id") String senderId, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
