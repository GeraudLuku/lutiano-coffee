package com.lutiano.api.models.response.audit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record AuditItemResponse(
    String action,
    @JsonProperty("actor_id") String actorId,
    @JsonProperty("actor_name") String actorName,
    @JsonProperty("actor_role") String actorRole,
    @JsonProperty("created_at") String createdAt,
    String details,
    @JsonProperty("entity_id") String entityId,
    @JsonProperty("entity_type") String entityType,
    String id,
    String ip,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
