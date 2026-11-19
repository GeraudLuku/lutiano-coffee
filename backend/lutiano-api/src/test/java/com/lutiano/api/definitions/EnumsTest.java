package com.lutiano.api.definitions;

import static org.junit.jupiter.api.Assertions.*;

import com.lutiano.api.definitions.enums.CommissionBasis;
import com.lutiano.api.definitions.enums.Role;
import com.lutiano.api.definitions.enums.SearchType;
import com.lutiano.api.security.Roles;
import com.lutiano.api.service.earnings.CommissionService;
import com.lutiano.api.utils.Enums;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Enums are the single list of allowed values; the string constants and sets used elsewhere must agree with them. */
class EnumsTest {
  @Test void namesKeepDeclarationOrder() {
    assertEquals(List.of("FIXED_PER_UNIT", "PERCENT"), List.copyOf(Enums.names(CommissionBasis.class)));
    assertEquals(List.of("FIXED_PER_UNIT", "PERCENT"), Enums.list(CommissionBasis.class));
  }

  @Test void roleConstantsMatchTheRoleEnum() {
    assertEquals(Role.ADMIN.name(), Roles.ADMIN);
    assertEquals(Role.INVENTORY_MANAGER.name(), Roles.INVENTORY_MANAGER);
    assertEquals(Role.SELLER.name(), Roles.SELLER);
    assertEquals(Role.CUSTOMER.name(), Roles.CUSTOMER);
    assertEquals(Enums.names(Role.class).size() - 1, Roles.ALL.size());   // every role but the customer is staff
  }

  @Test void validationSetsAreBuiltFromTheirEnums() {
    assertEquals(Enums.names(CommissionBasis.class), CommissionService.BASES);
    assertEquals(CommissionService.FIXED_PER_UNIT, CommissionBasis.FIXED_PER_UNIT.name());
    assertEquals(CommissionService.PERCENT, CommissionBasis.PERCENT.name());
  }

  @Test void searchTypesAreTheNamesStoredInTheIndex() {
    assertEquals(24, SearchType.values().length);
    assertEquals("PAYOUT_DESTINATION", SearchType.PAYOUT_DESTINATION.name());
  }
}
