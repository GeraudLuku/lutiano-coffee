package com.lutiano.api.models.response.staff;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record StaffOperationResponse(
    Boolean active,
    @JsonProperty("channel_matches") Boolean channelMatches,
    String code,
    String id,
    String name,
    String type,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
