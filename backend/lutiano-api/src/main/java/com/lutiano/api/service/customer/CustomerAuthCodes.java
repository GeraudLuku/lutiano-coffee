package com.lutiano.api.service.customer;

import com.lutiano.api.service.notification.NotificationCatalog;
import com.lutiano.api.service.otp.OtpService;
import org.springframework.stereotype.Component;

/** Sends the customer verification SMS code for a purpose, so every customer flow asks for codes the same way. */
@Component
public class CustomerAuthCodes {
  private final OtpService otp;

  public CustomerAuthCodes(OtpService otp) { this.otp = otp; }

  public void send(String purpose, String subject, String recipientId, String name, String phone, String language) {
    otp.start(purpose, subject, NotificationCatalog.OTP_CUSTOMER, recipientId, name, phone, language, false);
  }
}
