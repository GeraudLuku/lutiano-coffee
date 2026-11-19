package com.lutiano.api.models.response.catalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record OperationItemResponse(
    String address,
    String city,
    String code,
    Object description,
    String id,
    @JsonProperty("image_url") Object imageUrl,
    BigDecimal latitude,
    BigDecimal longitude,
    String name,
    @JsonProperty("open_now") Boolean openNow,
    String region,
    String type,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
