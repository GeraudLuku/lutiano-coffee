package com.lutiano.api.models.request.payment;

public record PaymentMethodRequest(String label, String merchantAccount, Boolean enabled) {}
