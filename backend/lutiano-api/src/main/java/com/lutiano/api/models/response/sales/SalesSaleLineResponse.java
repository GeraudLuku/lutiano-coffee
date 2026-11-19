package com.lutiano.api.models.response.sales;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record SalesSaleLineResponse(
    List<Object> addons,
    @JsonProperty("addons_unit_xaf") Long addonsUnitXaf,
    @JsonProperty("image_url") Object imageUrl,
    @JsonProperty("line_no") Long lineNo,
    @JsonProperty("line_total_xaf") Long lineTotalXaf,
    @JsonProperty("product_id") String productId,
    @JsonProperty("product_name") String productName,
    Long quantity,
    String sku,
    @JsonProperty("unit_price_xaf") Long unitPriceXaf,
    @JsonProperty("variant_name") Object variantName,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
