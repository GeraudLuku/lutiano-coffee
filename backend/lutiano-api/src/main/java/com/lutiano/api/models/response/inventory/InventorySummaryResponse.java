package com.lutiano.api.models.response.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record InventorySummaryResponse(
    Long consumed,
    Long net,
    Long received,
    @JsonProperty("top_consumed") List<InventorySummaryTopConsumedResponse> topConsumed,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
