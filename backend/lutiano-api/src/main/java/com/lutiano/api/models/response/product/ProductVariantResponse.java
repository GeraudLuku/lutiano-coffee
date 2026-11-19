package com.lutiano.api.models.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ProductVariantResponse(
    Boolean active,
    @JsonProperty("active_variant_count") Long activeVariantCount,
    String category,
    @JsonProperty("central_quantity") Long centralQuantity,
    @JsonProperty("created_at") String createdAt,
    String description,
    String id,
    @JsonProperty("image_url") Object imageUrl,
    String kind,
    @JsonProperty("low_stock_threshold") Long lowStockThreshold,
    @JsonProperty("loyalty_eligible") Boolean loyaltyEligible,
    @JsonProperty("min_variant_price") Object minVariantPrice,
    String name,
    @JsonProperty("parent_active") Boolean parentActive,
    @JsonProperty("parent_id") String parentId,
    @JsonProperty("parent_name") String parentName,
    @JsonProperty("price_xaf") Long priceXaf,
    @JsonProperty("promotion_eligible") Boolean promotionEligible,
    String sku,
    @JsonProperty("sort_order") Long sortOrder,
    @JsonProperty("track_stock") Boolean trackStock,
    String unit,
    @JsonProperty("updated_at") Object updatedAt,
    @JsonProperty("variant_count") Long variantCount,
    @JsonProperty("variant_name") String variantName,
    @JsonProperty("variants_central_quantity") Long variantsCentralQuantity,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
