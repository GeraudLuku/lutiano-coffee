package com.lutiano.api.models.response.business;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record SecurityResponse(
    Long codeLength,
    Long codeMinutes,
    Long maxAttempts,
    Boolean mfaEnabled,
    Long sessionHours,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
