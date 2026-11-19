package com.lutiano.api.models.response.customer;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ProviderResponse(Boolean apple, Boolean google, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
