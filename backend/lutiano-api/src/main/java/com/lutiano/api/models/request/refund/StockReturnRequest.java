package com.lutiano.api.models.request.refund;

import java.util.List;

public record StockReturnRequest(List<StockReturnLineRequest> lines, String note) {}
