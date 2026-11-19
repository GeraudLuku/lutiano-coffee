package com.lutiano.api.models.response.search;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record StatusOutboxResponse(
    Long failed,
    Long lastDoneId,
    Object oldestPending,
    Long pending,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
