package com.lutiano.api.models.request.operation;

public record OperatingHoursRequest(Integer weekday, String opens, String closes) {}
