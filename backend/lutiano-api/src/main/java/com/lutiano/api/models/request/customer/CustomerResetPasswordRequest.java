package com.lutiano.api.models.request.customer;

public record CustomerResetPasswordRequest(String phone, String code, String newPassword) {}
