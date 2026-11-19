package com.lutiano.api.controller.notification;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.security.CurrentUser;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.notification.InAppNotifications;
import com.lutiano.api.models.request.common.DeviceRequest;
import com.lutiano.api.models.request.common.IdsRequest;
import com.lutiano.api.models.request.common.PreferenceRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.notification.NotificationItemResponse;
import com.lutiano.api.models.response.customer.PreferenceResponse;
import com.lutiano.api.utils.Responses;
import com.lutiano.api.models.response.customer.UnreadCountResponse;

/** The signed-in person's own in-app notifications and preferences. */
@RestController
@RequestMapping("/me/notifications")
public class MyNotificationController {
  private static final String ID = "/{id:[0-9a-fA-F-]{36}}";
  private final InAppNotifications service;
  private final CurrentUser current;

  public MyNotificationController(InAppNotifications service, CurrentUser current) { this.service = service; this.current = current; }


  /** category (INVENTORY, REQUESTS, SALES, EARNINGS, TARGETS, OPERATIONS, SYSTEM), filter (UNREAD, IMPORTANT), days (last N days); the page summary counts everything not cleared. */
  @GetMapping
  public ResponseEntity<GenericApiResponse<PageResponse<NotificationItemResponse>>> list(@RequestParam(required = false) String category, @RequestParam(required = false) String filter,
                                        @RequestParam(required = false) Integer days, PageRequest paging, Authentication a) {
    return Responses.page(service.list(current.get(a), category, filter, days, paging), NotificationItemResponse.class);
  }

  @GetMapping("/unread-count")
  public ResponseEntity<GenericApiResponse<UnreadCountResponse>> unread(Authentication a) { return Responses.ok(service.unread(current.get(a)), UnreadCountResponse.class); }

  @GetMapping(ID)
  public ResponseEntity<GenericApiResponse<NotificationItemResponse>> get(@PathVariable String id, Authentication a) { return Responses.ok(service.get(current.get(a), id), NotificationItemResponse.class); }

  /** Marks the given notifications as read; with no ids, all of them. */
  @PostMapping("/read")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> read(@RequestBody(required = false) IdsRequest body, Authentication a) {
    return Responses.ok(service.markRead(current.get(a), body == null ? null : body.ids()));
  }

  /** Hides the given notifications; with no ids, every one already read. They are removed after 90 days. */
  @PostMapping("/clear")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> clear(@RequestBody(required = false) IdsRequest body, Authentication a) {
    return Responses.ok(service.clear(current.get(a), body == null ? null : body.ids()));
  }

  @GetMapping("/preferences")
  public ResponseEntity<GenericApiResponse<List<PreferenceResponse>>> preferences(Authentication a) { return Responses.list(service.preferences(current.get(a)), PreferenceResponse.class); }

  /** Switches the app (and push) on or off for one category. EARNINGS and OPERATIONS cannot be switched off in the app. */
  @PutMapping("/preferences/{category}")
  public ResponseEntity<GenericApiResponse<List<Map<String, Object>>>> save(@PathVariable String category, @RequestBody PreferenceRequest body, Authentication a) {
    return Responses.ok(service.savePreference(current.get(a), category, body.inApp(), body.push()));
  }

  /** Registers this phone for push notifications (Firebase token). Whoever signs in on a phone owns its token. */
  @PostMapping("/devices")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> register(@RequestBody DeviceRequest body, Authentication a) {
    return Responses.ok(service.registerDevice(current.get(a), body.token(), body.platform()));
  }

  /** Forgets this phone (on sign-out), so it stops receiving the person's notifications. */
  @DeleteMapping("/devices")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> unregister(@RequestParam String token, Authentication a) {
    return Responses.ok(service.unregisterDevice(current.get(a), token));
  }
}
