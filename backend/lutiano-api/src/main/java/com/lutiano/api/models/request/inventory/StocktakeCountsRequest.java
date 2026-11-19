package com.lutiano.api.models.request.inventory;

import java.util.List;

public record StocktakeCountsRequest(List<StocktakeCountRequest> counts) {}
