package com.lutiano.api.models.response.search;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record SuggestionResponse(
    String highlight,
    String id,
    String reference,
    String subtitle,
    String title,
    String type,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
