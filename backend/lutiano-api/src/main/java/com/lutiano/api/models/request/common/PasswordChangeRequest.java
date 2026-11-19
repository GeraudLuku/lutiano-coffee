package com.lutiano.api.models.request.common;

public record PasswordChangeRequest(String currentPassword, String newPassword) {}
