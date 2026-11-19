package com.lutiano.api.models.request.operation;

import java.util.List;

/** `code` is ignored: codes are generated on create and never change. latitude/longitude are required. */
public record OperationRequest(String name, String code, String type, String address, String city, String region, Double latitude, Double longitude, String description, Boolean active, List<String> paymentMethods) {}
