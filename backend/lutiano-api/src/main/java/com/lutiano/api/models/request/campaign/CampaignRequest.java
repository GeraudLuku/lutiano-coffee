package com.lutiano.api.models.request.campaign;

import java.time.LocalDate;
import java.util.List;

public record CampaignRequest(String name, String description, LocalDate startsOn, LocalDate endsOn, String operationId, List<CampaignTierRequest> tiers) {}
