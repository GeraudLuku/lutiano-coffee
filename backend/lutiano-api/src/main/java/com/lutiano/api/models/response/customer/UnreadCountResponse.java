package com.lutiano.api.models.response.customer;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record UnreadCountResponse(Long unread, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
