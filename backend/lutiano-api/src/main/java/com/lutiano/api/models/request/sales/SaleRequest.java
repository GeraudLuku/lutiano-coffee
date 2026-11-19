package com.lutiano.api.models.request.sales;

import java.util.List;
import com.lutiano.api.models.request.common.LineItemRequest;

/** customerToken: the customer's loyalty QR, scanned at checkout (customer-confirmed association). Optional. */
public record SaleRequest(String operationId, List<LineItemRequest> lines, Long cashReceivedXaf, String paymentMethod, String customerToken) {}
