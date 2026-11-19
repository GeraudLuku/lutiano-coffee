package com.lutiano.api.models.request.sales;

import java.util.List;
import com.lutiano.api.models.request.common.LineItemRequest;

/** An administrator's sale, entered for a seller with an open shift. */
public record SaleOnBehalfRequest(String sellerId, String operationId, List<LineItemRequest> lines, Long cashReceivedXaf, String paymentMethod, String customerToken) {}
