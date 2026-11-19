package com.lutiano.api.models.request.search;

import java.util.List;

public record SearchRequest(String q, List<String> types) {}
