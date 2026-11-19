package com.lutiano.api.models.response.search;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record StatusResponse(
    Boolean available,
    StatusDocumentsResponse documents,
    String index,
    StatusOutboxResponse outbox,
    StatusReindexResponse reindex,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
