package com.lutiano.api.models.response.operation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record OperationSellerResponse(
    Boolean active,
    String email,
    @JsonProperty("full_name") String fullName,
    String id,
    String phone,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
