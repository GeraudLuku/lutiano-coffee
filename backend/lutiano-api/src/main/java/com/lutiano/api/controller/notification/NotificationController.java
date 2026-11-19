package com.lutiano.api.controller.notification;

import static com.lutiano.api.security.Roles.ADMIN;

import java.time.LocalDate;
import java.util.Map;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.notification.NotificationAdminService;
import com.lutiano.api.models.request.notification.ApproversRequest;
import com.lutiano.api.models.request.notification.NotificationEventRequest;
import com.lutiano.api.models.request.notification.NotificationIdRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.notification.DeliveryItemResponse;
import com.lutiano.api.models.response.notification.EventItemResponse;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.notification.NotificationMineItemResponse;
import com.lutiano.api.utils.Responses;
import com.lutiano.api.models.response.notification.NotificationSettingResponse;

/** Notification settings and the delivery log. Administrators only. */
@RestController
@RequestMapping("/notifications")
public class NotificationController {
  private final NotificationAdminService service;
  private final CurrentUser current;

  public NotificationController(NotificationAdminService service, CurrentUser current) { this.service = service; this.current = current; }


  @GetMapping("/deliveries")
  public ResponseEntity<GenericApiResponse<PageResponse<DeliveryItemResponse>>> deliveries(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                                              @RequestParam(required = false) String status, @RequestParam(required = false) String event,
                                              @RequestParam(required = false) String q, PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.page(service.deliveries(from, to, status, event, q, paging), DeliveryItemResponse.class);
  }

  @PostMapping("/deliveries/{id:[0-9a-fA-F-]+}/resend")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> resend(@PathVariable String id, Authentication a) {
    return Responses.ok(service.resend(current.require(a, ADMIN), id));
  }

  @GetMapping("/events")
  public ResponseEntity<GenericApiResponse<PageResponse<EventItemResponse>>> events(@RequestParam(required = false) String q, PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.page(service.events(q, paging), EventItemResponse.class);
  }

  @PutMapping("/events/{key:[A-Z_]+}")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> saveEvent(@PathVariable String key, @RequestBody NotificationEventRequest body, Authentication a) {
    service.saveEvent(current.require(a, ADMIN), key, body);
    return Responses.ok(Map.of("status", "saved"));
  }

  /** The signed-in person's own messages. */
  @GetMapping("/mine")
  public ResponseEntity<GenericApiResponse<PageResponse<NotificationMineItemResponse>>> mine(@RequestParam(required = false) String topic, PageRequest paging, Authentication a) { return Responses.page(service.inbox(current.get(a), topic, paging), NotificationMineItemResponse.class); }

  /** Marks one message ({@code id}) or all of them as read. */
  @PostMapping("/mine/read")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> markRead(@RequestBody(required = false) NotificationIdRequest body, Authentication a) {
    service.markRead(current.get(a), body == null ? null : body.id());
    return Responses.ok(Map.of("status", "read"));
  }

  @GetMapping("/settings")
  public ResponseEntity<GenericApiResponse<NotificationSettingResponse>> settings(Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(service.settings(), NotificationSettingResponse.class);
  }

  @PutMapping("/settings/approvers")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> saveApprovers(@RequestBody ApproversRequest body, Authentication a) {
    service.saveApprovers(current.require(a, ADMIN), body.staffIds());
    return Responses.ok(service.settings());
  }
}
