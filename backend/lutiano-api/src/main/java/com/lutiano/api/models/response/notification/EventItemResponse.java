package com.lutiano.api.models.response.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record EventItemResponse(
    String audience,
    String category,
    String en,
    String fr,
    String key,
    Boolean locked,
    String name,
    List<String> placeholders,
    @JsonProperty("sms_enabled") Boolean smsEnabled,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
