package com.lutiano.api.models.response.reference;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record RegionResponse(List<String> cities, String name, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
