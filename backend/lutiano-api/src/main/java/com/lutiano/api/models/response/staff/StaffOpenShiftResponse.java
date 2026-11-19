package com.lutiano.api.models.response.staff;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record StaffOpenShiftResponse(
    @JsonProperty("expected_cash") Long expectedCash,
    String id,
    @JsonProperty("opened_at") String openedAt,
    @JsonProperty("opening_float") Long openingFloat,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
