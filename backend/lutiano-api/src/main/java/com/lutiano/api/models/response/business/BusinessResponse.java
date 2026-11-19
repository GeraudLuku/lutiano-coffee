package com.lutiano.api.models.response.business;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record BusinessResponse(
    Object address,
    Object description,
    Object email,
    String name,
    Object phone,
    @JsonProperty("updated_at") String updatedAt,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
