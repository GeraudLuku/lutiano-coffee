package com.lutiano.api.models.response.operation;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record OperationV2Response(
    Boolean active,
    String address,
    String city,
    String code,
    @JsonProperty("created_at") String createdAt,
    Object description,
    List<Object> hours,
    String id,
    @JsonProperty("image_url") Object imageUrl,
    BigDecimal latitude,
    BigDecimal longitude,
    String name,
    @JsonProperty("payment_methods") List<String> paymentMethods,
    String region,
    List<OperationSellerResponse> sellers,
    OperationStatsResponse stats,
    String type,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
