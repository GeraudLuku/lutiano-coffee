package com.lutiano.api.models.request.customer;

import java.time.LocalDate;

public record CustomerProfileRequest(String fullName, String email, String language, LocalDate birthDate, Boolean marketingConsent) {}
