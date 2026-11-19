package com.lutiano.api.models.response.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record NotificationSettingApproverResponse(
    @JsonProperty("approval_alerts") Boolean approvalAlerts,
    @JsonProperty("full_name") String fullName,
    @JsonProperty("has_phone") Boolean hasPhone,
    String id,
    String language,
    @JsonProperty("phone_masked") String phoneMasked,
    String role,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
