package com.lutiano.api.models.request.loyalty;

public record CustomerRequest(String fullName, String phone, Boolean phoneVerified, String status) {}
