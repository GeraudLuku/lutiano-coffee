package com.lutiano.api.service.business;

import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.security.Actor;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.lutiano.api.repository.business.BusinessRepository;
import com.lutiano.api.models.request.business.BusinessRequest;

@Service
public class BusinessService {

  private final BusinessRepository business;
  private final AuditService audit;

  public BusinessService(BusinessRepository business, AuditService audit) { this.business = business; this.audit = audit; }

  public Map<String, Object> get() { return business.find(); }

  public Map<String, Object> update(Actor actor, BusinessRequest in) {
    business.update(Validate.text(in.name(), "Business name", 150), Validate.optional(in.description(), "Description", 255),
        in.email() == null || in.email().isBlank() ? null : Validate.email(in.email()), Validate.optional(in.phone(), "Phone", 30),
        Validate.optional(in.address(), "Address", 255), actor.id());
    audit.log(actor, "BUSINESS_INFO_UPDATED", "settings", null, in.name());
    return business.find();
  }
}
