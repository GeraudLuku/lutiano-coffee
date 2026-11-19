package com.lutiano.api.service.search;

import com.lutiano.api.security.Actor;
import com.lutiano.api.security.Roles;
import java.util.List;
import com.lutiano.api.definitions.enums.PrincipalKind;

/**
 * Who is searching, resolved server-side (never from the request). {@code CUSTOMER} with no id is the anonymous
 * public catalogue caller. Unknown roles match nothing (fail closed).
 */
public record SearchPrincipal(PrincipalKind kind, String id, String role, List<String> operationIds) {
    public static SearchPrincipal staff(Actor a, List<String> assignedOperationIds) {
    return new SearchPrincipal(PrincipalKind.STAFF, a.id(), a.role(), a.isSeller() ? assignedOperationIds : List.of());
  }

  public static SearchPrincipal publicCatalogue() { return new SearchPrincipal(PrincipalKind.CUSTOMER, null, null, List.of()); }

  public boolean isAdmin() { return kind == PrincipalKind.STAFF && Roles.ADMIN.equals(role); }
  public boolean isInventoryManager() { return kind == PrincipalKind.STAFF && Roles.INVENTORY_MANAGER.equals(role); }
  public boolean isSeller() { return kind == PrincipalKind.STAFF && Roles.SELLER.equals(role); }
  public boolean isCustomer() { return kind == PrincipalKind.CUSTOMER; }
}
