package com.lutiano.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Every {@code lutiano.*} setting in one typed place (see application.yml for the keys, defaults and the environment
 * variables that fill them). Classes take only the group they need, e.g. {@code AppProperties.Otp}.
 */
@ConfigurationProperties(prefix = "lutiano")
public record AppProperties(Jwt jwt, Cors cors, Storage storage, Social social, Sellers sellers, Refunds refunds, Sms sms, Otp otp, Mfa mfa,
                            Search search, Push push, @DefaultValue Orders orders, @DefaultValue Rewards rewards, @DefaultValue Dashboard dashboard,
                            @DefaultValue Inventory inventory) {

  /** Signing key (32+ characters) and how long a sign-in lasts. */
  public record Jwt(String secret, int ttlHours) {}

  /** Origins allowed to call the API from a browser, comma-separated. */
  public record Cors(String allowedOrigins) {}

  /** Any S3-compatible store: images in {@code bucket}, seller documents in the private {@code documentsBucket}. */
  public record Storage(String endpoint, String region, String accessKey, String secretKey, String bucket, String documentsBucket) {}

  /** Customer sign-in with Google or Apple; a provider with no ids is switched off. */
  public record Social(Google google, Apple apple) {
    public record Google(@DefaultValue("") String clientIds) {}
    public record Apple(@DefaultValue("") String audiences) {}
  }

  public record Sellers(String requiredDocuments, @DefaultValue("false") boolean enforceEligibility) {}

  public record Refunds(@DefaultValue("true") boolean secondApprover) {}

  /** Hombee SMS: in "dev" mode (or without an API key) messages are logged instead of sent. */
  public record Sms(String mode, String baseUrl, @DefaultValue("") String apiKey, String senderId, int connectTimeoutMs, int readTimeoutMs) {}

  /** One-time codes. {@code devCode} is accepted only while SMS is not live. */
  public record Otp(int length, int ttlSeconds, int maxAttempts, int resendCooldownSeconds, int maxPerHour, @DefaultValue("") String devCode) {}

  public record Mfa(boolean enabled) {}

  /** Elasticsearch search: indexing switch, batch and retry sizes, and the caps on candidates, verified results and query length. */
  public record Search(boolean indexingEnabled, int batchSize, int retryMaxAttempts, int candidateCap, int verificationCap, int maxQueryLength) {}

  /** Firebase service-account file for push notifications; without the file, push is off. */
  public record Push(@DefaultValue("") String credentials) {}

  public record Orders(@DefaultValue("30") int acceptTimeoutMinutes) {}

  public record Rewards(@DefaultValue("30") int walletValidityDays) {}

  public record Dashboard(@DefaultValue("14") int inactivityDays) {}

  public record Inventory(@DefaultValue("true") boolean secondApprover) {}
}
