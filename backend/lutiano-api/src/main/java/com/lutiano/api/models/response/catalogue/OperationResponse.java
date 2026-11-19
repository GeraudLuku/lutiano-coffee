package com.lutiano.api.models.response.catalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record OperationResponse(
    String address,
    String city,
    String code,
    Object description,
    List<Object> hours,
    String id,
    @JsonProperty("image_url") Object imageUrl,
    BigDecimal latitude,
    BigDecimal longitude,
    String name,
    @JsonProperty("open_now") Boolean openNow,
    @JsonProperty("payment_methods") List<String> paymentMethods,
    String region,
    String type,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
