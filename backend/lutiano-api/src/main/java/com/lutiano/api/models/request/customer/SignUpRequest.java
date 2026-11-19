package com.lutiano.api.models.request.customer;

public record SignUpRequest(String fullName, String phone, String email, String password, String language, Boolean acceptTerms) {}
