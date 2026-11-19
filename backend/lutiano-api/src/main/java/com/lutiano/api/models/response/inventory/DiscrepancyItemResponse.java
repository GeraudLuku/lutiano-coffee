package com.lutiano.api.models.response.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record DiscrepancyItemResponse(
    @JsonProperty("allocation_id") String allocationId,
    @JsonProperty("confirmed_by_name") String confirmedByName,
    @JsonProperty("created_at") String createdAt,
    String id,
    String note,
    @JsonProperty("operation_name") String operationName,
    @JsonProperty("product_name") String productName,
    @JsonProperty("received_quantity") Long receivedQuantity,
    @JsonProperty("sent_quantity") Long sentQuantity,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
