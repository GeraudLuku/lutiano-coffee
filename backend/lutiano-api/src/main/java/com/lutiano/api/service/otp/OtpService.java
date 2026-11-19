package com.lutiano.api.service.otp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.service.notification.NotificationService;
import com.lutiano.api.proxy.SmsUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.security.NoSuchAlgorithmException;
import com.lutiano.api.config.AppProperties;
import com.lutiano.api.definitions.enums.SmsStatus;

/**
 * One-time SMS codes kept in Redis, so they expire by themselves. A code is stored only as a hash, is valid for a few
 * minutes and a limited number of tries, and can be requested again only after a short wait and a few times per hour.
 * A code is identified by its {@code purpose} (e.g. "signin") and {@code subject} (the person or record it is for).
 */
@Service
public class OtpService {
  private static final SecureRandom RANDOM = new SecureRandom();

  private final StringRedisTemplate redis;
  private final NotificationService notifications;
  private final SmsUtil sms;
  private final int length, maxAttempts, maxPerHour;
  private final Duration ttl, cooldown;
  private final String devCode, pepper;

  public OtpService(StringRedisTemplate redis, NotificationService notifications, SmsUtil sms, AppProperties.Otp otp, AppProperties.Jwt jwt) {
    this.redis = redis; this.notifications = notifications; this.sms = sms; this.length = otp.length(); this.maxAttempts = otp.maxAttempts();
    this.maxPerHour = otp.maxPerHour(); this.ttl = Duration.ofSeconds(otp.ttlSeconds()); this.cooldown = Duration.ofSeconds(otp.resendCooldownSeconds());
    this.devCode = otp.devCode(); this.pepper = jwt.secret();
  }

  /** Seconds a person must wait before asking for another code. */
  public int cooldownSeconds() { return (int) cooldown.toSeconds(); }

  /**
   * Creates a code and sends it with the SMS event {@code event} to {@code phone}. Fails with a clear message when codes
   * are asked for too often or the SMS could not be sent (the code is then discarded, so the person can try again).
   * With {@code reuseRecent}, asking again during the wait keeps the code that was just sent instead of failing.
   */
  public void start(String purpose, String subject, String event, String recipientId, String name, String phone, String language, boolean reuseRecent) {
    String key = key("otp", purpose, subject), cooldownKey = key("cd", purpose, subject), hourKey = key("hr", purpose, subject);
    Long wait = redis.getExpire(cooldownKey);
    if (Boolean.TRUE.equals(redis.hasKey(cooldownKey))) {
      if (reuseRecent && Boolean.TRUE.equals(redis.hasKey(key))) return;
      throw Validate.tooMany("Wait " + Math.max(wait == null ? 1 : wait, 1) + " seconds before asking for a new code.");
    }
    Long asked = redis.opsForValue().increment(hourKey);
    if (asked != null && asked == 1) redis.expire(hourKey, Duration.ofHours(1));
    // The hourly limit protects the SMS budget, so it applies only when messages really go out.
    if (sms.isLive() && asked != null && asked > maxPerHour) throw Validate.tooMany("Too many codes were requested. Try again in an hour.");
    String code = code();
    redis.opsForHash().putAll(key, Map.of("h", hash(purpose, subject, code), "a", "0"));
    redis.expire(key, ttl);
    SmsUtil.Result sent = notifications.sendCode(event, recipientId, name, phone, language, Map.of("code", code));
    if (sent.status() == SmsStatus.FAILED) {
      redis.delete(key);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "We could not send the code. Try again in a moment.");
    }
    redis.opsForValue().set(cooldownKey, "1", cooldown);
  }

  /** Checks a code, using up one try. Succeeds silently (the code is then spent); otherwise explains what is wrong. */
  public void verify(String purpose, String subject, String code) {
    String key = key("otp", purpose, subject);
    Object stored = redis.opsForHash().get(key, "h");
    if (stored == null) throw Validate.bad("This code has expired, or none was requested. Ask for a new one.");
    Long attempts = redis.opsForHash().increment(key, "a", 1);
    if (attempts != null && attempts > maxAttempts) {
      redis.delete(key);
      throw Validate.tooMany("Too many wrong codes. Ask for a new one.");
    }
    String given = code == null ? "" : code.trim();
    boolean devAccepted = !sms.isLive() && !devCode.isBlank() && devCode.equals(given);
    if (!devAccepted && !MessageDigest.isEqual(stored.toString().getBytes(StandardCharsets.UTF_8), hash(purpose, subject, given).getBytes(StandardCharsets.UTF_8)))
      throw Validate.bad("That code is not correct. " + Math.max(maxAttempts - (attempts == null ? 0 : attempts), 0) + " tries left.");
    // The code is spent, so the next sign-in may ask for a new one straight away.
    redis.delete(List.of(key, key("cd", purpose, subject)));
  }

  private String code() {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < length; i++) b.append(RANDOM.nextInt(10));
    return b.toString();
  }

  private String hash(String purpose, String subject, String code) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((pepper + ":" + purpose + ":" + subject + ":" + code).getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
  }

  private static String key(String kind, String purpose, String subject) { return kind + ":" + purpose + ":" + subject; }
}
