package com.lutiano.api.models.request.auth;

public record MfaCodeRequest(String challenge, String code) {}
