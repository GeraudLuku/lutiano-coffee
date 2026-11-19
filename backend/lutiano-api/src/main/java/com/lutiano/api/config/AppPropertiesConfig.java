package com.lutiano.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Makes each settings group of {@link AppProperties} injectable on its own, so a class asks only for what it uses. */
@Configuration
public class AppPropertiesConfig {
  @Bean AppProperties.Jwt jwtProperties(AppProperties p) { return p.jwt(); }
  @Bean AppProperties.Cors corsProperties(AppProperties p) { return p.cors(); }
  @Bean AppProperties.Storage storageProperties(AppProperties p) { return p.storage(); }
  @Bean AppProperties.Social socialProperties(AppProperties p) { return p.social(); }
  @Bean AppProperties.Sellers sellersProperties(AppProperties p) { return p.sellers(); }
  @Bean AppProperties.Refunds refundsProperties(AppProperties p) { return p.refunds(); }
  @Bean AppProperties.Sms smsProperties(AppProperties p) { return p.sms(); }
  @Bean AppProperties.Otp otpProperties(AppProperties p) { return p.otp(); }
  @Bean AppProperties.Mfa mfaProperties(AppProperties p) { return p.mfa(); }
  @Bean AppProperties.Search searchProperties(AppProperties p) { return p.search(); }
  @Bean AppProperties.Push pushProperties(AppProperties p) { return p.push(); }
  @Bean AppProperties.Orders ordersProperties(AppProperties p) { return p.orders(); }
  @Bean AppProperties.Rewards rewardsProperties(AppProperties p) { return p.rewards(); }
  @Bean AppProperties.Dashboard dashboardProperties(AppProperties p) { return p.dashboard(); }
  @Bean AppProperties.Inventory inventoryProperties(AppProperties p) { return p.inventory(); }
}
