package com.lutiano.api.models.response.order;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record OrderTimelineResponse(String at, String status, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
