package com.lutiano.api.utils;

import java.util.Set;

/**
 * The records the search service proposes for a list page's text box. The list still applies its own filters,
 * permissions, sorting, paging and totals in MariaDB; this only decides which ids the text matches.
 * {@code truncated} means more matched than the search service's cap, so the list asks the user to refine.
 */
public record Candidates(String q, Set<String> ids, boolean truncated) {}
