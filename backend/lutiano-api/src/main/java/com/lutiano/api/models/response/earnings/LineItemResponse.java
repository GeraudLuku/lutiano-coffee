package com.lutiano.api.models.response.earnings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record LineItemResponse(
    String basis,
    @JsonProperty("commission_xaf") Long commissionXaf,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("line_no") Long lineNo,
    @JsonProperty("line_value_xaf") Long lineValueXaf,
    @JsonProperty("operation_name") String operationName,
    @JsonProperty("product_id") String productId,
    @JsonProperty("product_name") String productName,
    Long quantity,
    Long rate,
    @JsonProperty("reversed_xaf") Long reversedXaf,
    @JsonProperty("rule_id") String ruleId,
    @JsonProperty("sale_created_at") String saleCreatedAt,
    @JsonProperty("sale_id") String saleId,
    @JsonProperty("sale_reference") String saleReference,
    @JsonProperty("seller_id") String sellerId,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
