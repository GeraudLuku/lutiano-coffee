package com.lutiano.api.models.response.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ActivityItemResponse(String action, String client, @JsonProperty("created_at") String createdAt, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
