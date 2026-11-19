package com.lutiano.api.models.response.earnings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record EarningsOverviewPeriodResponse(
    @JsonProperty("can_finalize") Boolean canFinalize,
    @JsonProperty("ends_on") String endsOn,
    @JsonProperty("finalized_at") Object finalizedAt,
    @JsonProperty("finalized_by") Object finalizedBy,
    @JsonProperty("finalized_by_name") Object finalizedByName,
    String id,
    @JsonProperty("starts_on") String startsOn,
    String status,
    EarningsOverviewPeriodTotalsResponse totals,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
