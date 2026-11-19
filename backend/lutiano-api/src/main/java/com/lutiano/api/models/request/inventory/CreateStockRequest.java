package com.lutiano.api.models.request.inventory;

import java.time.LocalDate;
import java.util.List;

public record CreateStockRequest(String operationId, List<StockRequestItemRequest> items, LocalDate neededBy, String reason, String idempotencyKey) {}
