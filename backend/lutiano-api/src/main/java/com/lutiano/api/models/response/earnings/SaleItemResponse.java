package com.lutiano.api.models.response.earnings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record SaleItemResponse(
    @JsonProperty("commission_xaf") Long commissionXaf,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("operation_name") String operationName,
    String reference,
    @JsonProperty("reversed_xaf") Long reversedXaf,
    @JsonProperty("sale_id") String saleId,
    @JsonProperty("total_xaf") Long totalXaf,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
