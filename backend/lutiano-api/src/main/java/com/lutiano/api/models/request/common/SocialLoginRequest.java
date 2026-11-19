package com.lutiano.api.models.request.common;

public record SocialLoginRequest(String provider, String idToken, String nonce) {}
