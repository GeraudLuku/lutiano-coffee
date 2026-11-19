package com.lutiano.api.service.search;

import com.lutiano.api.utils.Validate;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Per-user fixed-window limit kept in Redis, so search and exact phone/email lookups cannot be used to enumerate data. */
@Component
public class SearchRateLimiter {
  private final StringRedisTemplate redis;

  public SearchRateLimiter(StringRedisTemplate redis) { this.redis = redis; }

  /** Counts one call for {@code userId} in the current minute; throws 429 beyond {@code perMinute}. */
  public void check(String bucket, String userId, int perMinute) {
    String key = "search:rl:" + bucket + ":" + userId + ":" + (System.currentTimeMillis() / 60_000);
    Long n = redis.opsForValue().increment(key);
    if (n != null && n == 1) redis.expire(key, Duration.ofSeconds(90));
    if (n != null && n > perMinute) throw Validate.tooMany("Too many searches. Wait a moment and try again.");
  }
}
