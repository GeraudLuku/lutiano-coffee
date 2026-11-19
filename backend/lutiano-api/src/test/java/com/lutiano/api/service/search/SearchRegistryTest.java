package com.lutiano.api.service.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lutiano.api.api.ApiIntegrationTest;
import com.lutiano.api.definitions.enums.SearchType;
import com.lutiano.api.utils.Enums;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Every searchable type has exactly one indexing handler and one permission policy, so no type can be added half way. */
class SearchRegistryTest extends ApiIntegrationTest {
  @Autowired List<SearchHandler> handlers;
  @Autowired SearchPolicies policies;

  @Test void everySearchTypeHasAHandlerAndAPolicy() {
    var all = Enums.names(SearchType.class);
    assertEquals(all, handlers.stream().map(SearchHandler::type).collect(Collectors.toSet()));
    assertEquals(all, policies.types());
    assertEquals(handlers.size(), all.size());
  }
}
