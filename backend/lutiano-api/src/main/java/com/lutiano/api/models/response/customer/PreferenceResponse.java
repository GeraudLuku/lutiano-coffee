package com.lutiano.api.models.response.customer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record PreferenceResponse(
    String category,
    @JsonProperty("in_app") Boolean inApp,
    Boolean locked,
    Boolean push,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
