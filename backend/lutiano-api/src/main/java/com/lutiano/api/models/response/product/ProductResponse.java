package com.lutiano.api.models.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record ProductResponse(
    Boolean active,
    @JsonProperty("active_variant_count") Long activeVariantCount,
    @JsonProperty("addon_ids") List<String> addonIds,
    List<ProductAddonResponse> addons,
    String category,
    @JsonProperty("central_quantity") Long centralQuantity,
    @JsonProperty("created_at") String createdAt,
    String description,
    String id,
    @JsonProperty("image_url") Object imageUrl,
    String kind,
    @JsonProperty("low_stock_threshold") Long lowStockThreshold,
    @JsonProperty("loyalty_eligible") Boolean loyaltyEligible,
    @JsonProperty("min_variant_price") Long minVariantPrice,
    String name,
    @JsonProperty("parent_active") Object parentActive,
    @JsonProperty("parent_id") Object parentId,
    @JsonProperty("parent_name") Object parentName,
    @JsonProperty("price_xaf") Long priceXaf,
    @JsonProperty("promotion_eligible") Boolean promotionEligible,
    String sku,
    @JsonProperty("sort_order") Long sortOrder,
    ProductStatsResponse stats,
    List<ProductStockResponse> stock,
    @JsonProperty("track_stock") Boolean trackStock,
    String unit,
    @JsonProperty("updated_at") Object updatedAt,
    @JsonProperty("variant_count") Long variantCount,
    @JsonProperty("variant_name") Object variantName,
    List<ProductVariantResponse> variants,
    @JsonProperty("variants_central_quantity") Long variantsCentralQuantity,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
