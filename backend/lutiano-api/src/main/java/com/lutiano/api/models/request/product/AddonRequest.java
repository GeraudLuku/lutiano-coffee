package com.lutiano.api.models.request.product;

public record AddonRequest(String name, Long priceXaf, Integer sortOrder, Boolean active) {}
