package com.lutiano.api.models.response.health;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record HealthResponse(String service, String status, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
