package com.lutiano.api.models.response.product;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record AddonProductResponse(String id, String name, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
