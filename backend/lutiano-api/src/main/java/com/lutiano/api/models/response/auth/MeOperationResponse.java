package com.lutiano.api.models.response.auth;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record MeOperationResponse(
    Boolean active,
    String code,
    String id,
    String name,
    String type,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
