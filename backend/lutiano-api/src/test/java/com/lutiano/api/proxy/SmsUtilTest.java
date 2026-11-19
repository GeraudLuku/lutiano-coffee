package com.lutiano.api.proxy;

import static org.junit.jupiter.api.Assertions.*;

import com.lutiano.api.config.AppProperties;
import org.junit.jupiter.api.Test;

class SmsUtilTest {
  private static AppProperties.Sms sms(String mode, String key) { return new AppProperties.Sms(mode, "http://localhost:1", key, "LUTIANO", 1000, 1000); }
  private static AppProperties.Otp otp(String devCode) { return new AppProperties.Otp(6, 300, 5, 60, 5, devCode); }

  @Test void liveModeRefusesToStartWhileADevelopmentCodeIsSet() {
    assertThrows(IllegalStateException.class, () -> new SmsUtil(sms("live", "key"), otp("123456")));
  }

  @Test void liveModeWithAKeyIsLiveAndDevModeNever() {
    assertTrue(new SmsUtil(sms("live", "key"), otp("")).isLive());
    assertFalse(new SmsUtil(sms("dev", "key"), otp("123456")).isLive());
    assertFalse(new SmsUtil(sms("live", ""), otp("")).isLive());   // live without a key only logs
  }
}
