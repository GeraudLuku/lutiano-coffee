package com.lutiano.api.models.request.order;

import java.util.List;
import com.lutiano.api.models.request.common.LineItemRequest;

public record PlaceOrderRequest(String operationId, List<LineItemRequest> lines, String note) {}
