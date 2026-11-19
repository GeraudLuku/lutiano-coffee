package com.lutiano.api.service.inventory;

import com.lutiano.api.utils.Validate;
import com.lutiano.api.security.Actor;
import org.springframework.stereotype.Component;
import com.lutiano.api.config.AppProperties;

/**
 * Separation of duties for stock approvals: by default the person who created a transfer request,
 * adjustment or stocktake cannot approve it (lutiano.inventory.second-approver).
 */
@Component
public class ApprovalPolicy {
  private final boolean secondApprover;

  public ApprovalPolicy(AppProperties.Inventory inventory) { this.secondApprover = inventory.secondApprover(); }

  public boolean requiresSecondApprover() { return secondApprover; }

  public void requireDifferentApprover(Actor approver, Object creatorId, String what) {
    if (secondApprover && approver.id().equals(creatorId))
      throw Validate.forbidden("Another manager must approve " + what + " that you created.");
  }
}
