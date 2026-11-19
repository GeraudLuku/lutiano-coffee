package com.lutiano.api.models.request.inventory;

/** operationId null = central stock. */
public record StartStocktakeRequest(String operationId, String note) {}
