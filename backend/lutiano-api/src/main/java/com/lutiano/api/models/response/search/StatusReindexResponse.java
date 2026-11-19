package com.lutiano.api.models.response.search;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record StatusReindexResponse(String state, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
