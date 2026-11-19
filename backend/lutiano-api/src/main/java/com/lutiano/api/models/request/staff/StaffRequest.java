package com.lutiano.api.models.request.staff;

/** sellerRole is required for SELLER accounts (FIXED_POS_BARISTA, MOBILE_BARISTA, MARKET_AGENT) and ignored otherwise. */
public record StaffRequest(String email, String fullName, String phone, String role, String sellerRole, String address, String area, String password, String language) {}
