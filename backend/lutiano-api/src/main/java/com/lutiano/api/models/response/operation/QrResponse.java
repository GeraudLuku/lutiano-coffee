package com.lutiano.api.models.response.operation;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record QrResponse(
    Boolean active,
    String code,
    String id,
    String name,
    String opaqueToken,
    String type,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
