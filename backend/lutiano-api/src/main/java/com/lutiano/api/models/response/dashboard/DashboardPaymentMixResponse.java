package com.lutiano.api.models.response.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record DashboardPaymentMixResponse(@JsonProperty("payment_method") String paymentMethod, @JsonProperty("revenue_xaf") Long revenueXaf, Long sales, @JsonAnySetter @JsonAnyGetter Map<String, Object> other) {}
