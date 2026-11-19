package com.lutiano.api.models.response.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record OrderLineResponse(
    List<Object> addons,
    @JsonProperty("addons_unit_xaf") Long addonsUnitXaf,
    @JsonProperty("line_no") Long lineNo,
    @JsonProperty("product_id") String productId,
    @JsonProperty("product_name") String productName,
    Long quantity,
    @JsonProperty("unit_price_xaf") Long unitPriceXaf,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
