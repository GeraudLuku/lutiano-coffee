package com.lutiano.api.models.request.inventory;

public record StocktakeCountRequest(String productId, Integer countedQuantity) {}
