package com.lutiano.api.models.response.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record OrderReorderResponse(
    @JsonProperty("addon_ids") List<Object> addonIds,
    List<Object> addons,
    Boolean available,
    String name,
    @JsonProperty("product_id") String productId,
    Long quantity,
    @JsonProperty("unit_xaf") Long unitXaf,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
