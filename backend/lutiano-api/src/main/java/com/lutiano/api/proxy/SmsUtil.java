package com.lutiano.api.proxy;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.lutiano.api.utils.Phones;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import com.lutiano.api.config.AppProperties;
import com.lutiano.api.definitions.enums.SmsStatus;

/**
 * The only door to the Hombee SMS API. Business code never calls it directly (see NotificationService) and a failure
 * here never throws: it comes back as a FAILED result, so the action that wanted to send a message still completes.
 * <p>
 * In "dev" mode, or without an API key, the message is written to the log instead of being sent. There are no
 * automatic retries: a timeout may still mean Hombee accepted the message, and a retry would then send it twice.
 */
@Component
public class SmsUtil {
    /** {@code detail} explains a failure (or a dev-mode log); it never contains the message text. */
  public record Result(SmsStatus status, String detail) {}

  private static final Logger log = LoggerFactory.getLogger(SmsUtil.class);
  private static final int MAX_DETAIL = 400;

  private final boolean live;
  private final String senderId;
  private final RestClient client;

  public SmsUtil(AppProperties.Sms config, AppProperties.Otp otp) {
    String mode = config.mode(), baseUrl = config.baseUrl(), apiKey = config.apiKey(), senderId = config.senderId(), devCode = otp.devCode();
    int connectTimeoutMs = config.connectTimeoutMs(), readTimeoutMs = config.readTimeoutMs();
    boolean liveMode = "live".equalsIgnoreCase(mode);
    if (liveMode && !devCode.isBlank()) throw new IllegalStateException("lutiano.otp.dev-code must be empty when lutiano.sms.mode is live.");
    this.live = liveMode && !apiKey.isBlank();
    if (liveMode && apiKey.isBlank()) log.warn("SMS mode is live but HOMBEE_SMS_API_KEY is empty: messages will only be logged.");
    this.senderId = senderId;
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeoutMs)).build());
    factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
    this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).defaultHeader("Authorization", "Bearer " + apiKey).build();
  }

  /** True when messages really leave the platform (live mode with an API key). */
  public boolean isLive() { return live; }

  public String senderId() { return senderId; }

  /**
   * Sends one message. {@code sensitive} messages (one-time codes) are logged without their text in live mode; in dev
   * mode the text is logged on purpose, so a developer can read the code.
   */
  public Result send(String phone, String message, boolean sensitive) {
    if (!live) {
      log.info("SMS (dev mode, not sent) to {}: {}", Phones.mask(phone), message);
      return new Result(SmsStatus.LOGGED, "Dev mode: written to the log, not sent.");
    }
    try {
      Map<String, Object> body = Map.of("senderId", senderId, "messages", List.of(Map.of("to", phone.substring(1), "message", message)));
      log.info("Hombee send-bulk request: to={} text={}", Phones.mask(phone), sensitive ? "[hidden]" : message);
      JsonNode response = client.post().uri("/sms/send-bulk").body(body).retrieve().body(JsonNode.class);
      log.info("Hombee send-bulk response: {}", response);
      return new Result(SmsStatus.SENT, null);
    } catch (RestClientResponseException e) {
      String detail = "HTTP " + e.getStatusCode().value() + ": " + truncate(e.getResponseBodyAsString());
      log.error("Hombee send-bulk failed to {}: {}", Phones.mask(phone), detail);
      return new Result(SmsStatus.FAILED, detail);
    } catch (Exception e) {
      log.error("Hombee send-bulk failed to {}: {}", Phones.mask(phone), e.toString());
      return new Result(SmsStatus.FAILED, truncate(e.getClass().getSimpleName() + ": " + e.getMessage()));
    }
  }

  private static String truncate(String s) { return s == null ? "" : s.length() > MAX_DETAIL ? s.substring(0, MAX_DETAIL) : s; }
}
