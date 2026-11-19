package com.lutiano.api.models.response.catalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record CategoryItemResponse(
    String description,
    String id,
    @JsonProperty("image_url") Object imageUrl,
    String name,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
