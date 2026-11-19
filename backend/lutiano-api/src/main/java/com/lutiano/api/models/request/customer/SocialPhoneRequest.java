package com.lutiano.api.models.request.customer;

public record SocialPhoneRequest(String ticket, String phone, String fullName, Boolean acceptTerms, String language) {}
