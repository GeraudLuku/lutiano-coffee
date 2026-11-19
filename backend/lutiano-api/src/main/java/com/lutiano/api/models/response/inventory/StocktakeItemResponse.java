package com.lutiano.api.models.response.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record StocktakeItemResponse(
    @JsonProperty("counted_count") Long countedCount,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("created_by") String createdBy,
    @JsonProperty("created_by_name") String createdByName,
    @JsonProperty("decided_at") String decidedAt,
    @JsonProperty("decided_by") String decidedBy,
    @JsonProperty("decided_by_name") String decidedByName,
    @JsonProperty("decision_note") String decisionNote,
    String id,
    @JsonProperty("line_count") Long lineCount,
    @JsonProperty("location_code") String locationCode,
    @JsonProperty("location_name") String locationName,
    String note,
    @JsonProperty("operation_id") String operationId,
    String status,
    @JsonProperty("submitted_at") String submittedAt,
    @JsonProperty("submitted_by") String submittedBy,
    @JsonProperty("submitted_by_name") String submittedByName,
    @JsonProperty("variance_count") Long varianceCount,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
