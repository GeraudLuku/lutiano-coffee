package com.lutiano.api.models.request.common;

import java.util.List;

/** addonIds are optional; the same product with different add-ons makes separate lines. */
public record LineItemRequest(String productId, Integer quantity, List<String> addonIds) {}
