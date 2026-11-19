package com.lutiano.api.models.response.earnings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record PeriodItemResponse(
    @JsonProperty("ends_on") String endsOn,
    @JsonProperty("finalized_at") String finalizedAt,
    @JsonProperty("finalized_by") String finalizedBy,
    @JsonProperty("finalized_by_name") String finalizedByName,
    String id,
    @JsonProperty("payable_xaf") Long payableXaf,
    @JsonProperty("starts_on") String startsOn,
    String status,
    @JsonProperty("total_xaf") Long totalXaf,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
