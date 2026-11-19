package com.lutiano.api.models.response.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record DashboardResponse(
    Long activeOperations,
    DashboardAttentionResponse attention,
    DashboardCurrentResponse current,
    Long days,
    @JsonInclude(JsonInclude.Include.NON_NULL) DashboardFinanceResponse finance,
    String from,
    List<DashboardInactiveSellerResponse> inactiveSellers,
    Long inactivityDays,
    List<DashboardInventoryStatusResponse> inventoryStatus,
    Long inventoryValueXaf,
    Long lowStockItems,
    Long openShifts,
    List<DashboardPaymentMixResponse> paymentMix,
    DashboardCurrentResponse previous,
    List<DashboardSalesByChannelResponse> salesByChannel,
    List<DashboardSalesByDayResponse> salesByDay,
    String to,
    List<DashboardTopProductResponse> topProducts,
    List<DashboardTopSellerResponse> topSellers,
    @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
