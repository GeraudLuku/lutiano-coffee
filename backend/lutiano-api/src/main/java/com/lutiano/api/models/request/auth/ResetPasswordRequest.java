package com.lutiano.api.models.request.auth;

public record ResetPasswordRequest(String email, String code, String newPassword) {}
