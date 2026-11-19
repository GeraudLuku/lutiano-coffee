package com.lutiano.api.models.request.product;

public record CategoryRequest(String name, String description, Integer sortOrder, Boolean active) {}
