package com.lutiano.api.controller.customer;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.service.notification.InAppNotifications;
import com.lutiano.api.security.CurrentCustomer;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.models.request.common.DeviceRequest;
import com.lutiano.api.models.request.common.IdsRequest;
import com.lutiano.api.models.request.common.PreferenceRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.customer.PreferenceResponse;
import com.lutiano.api.utils.Responses;
import com.lutiano.api.models.response.customer.UnreadCountResponse;

/** A customer's inbox, push devices and notification choices: the same service sellers use, for the customer's own categories. */
@RestController
@RequestMapping("/customer/notifications")
public class CustomerNotificationController {
  private static final String ID = "/{id:[0-9a-fA-F-]{36}}";
  private final InAppNotifications service;
  private final CurrentCustomer current;

  public CustomerNotificationController(InAppNotifications service, CurrentCustomer current) { this.service = service; this.current = current; }


  /** category (ORDERS, LOYALTY, PROMOTIONS, SYSTEM), filter (UNREAD, IMPORTANT), days (last N days). */
  @GetMapping
  public ResponseEntity<GenericApiResponse<PageResponse<Map<String, Object>>>> list(@RequestParam(required = false) String category, @RequestParam(required = false) String filter,
                                        @RequestParam(required = false) Integer days, PageRequest paging, Authentication a) {
    return Responses.ok(service.list(current.get(a), category, filter, days, paging));
  }

  @GetMapping("/unread-count")
  public ResponseEntity<GenericApiResponse<UnreadCountResponse>> unread(Authentication a) { return Responses.ok(service.unread(current.get(a)), UnreadCountResponse.class); }

  @GetMapping(ID)
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> get(@PathVariable String id, Authentication a) { return Responses.ok(service.get(current.get(a), id)); }

  @PostMapping("/read")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> read(@RequestBody(required = false) IdsRequest body, Authentication a) { return Responses.ok(service.markRead(current.get(a), body == null ? null : body.ids())); }

  @PostMapping("/clear")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> clear(@RequestBody(required = false) IdsRequest body, Authentication a) { return Responses.ok(service.clear(current.get(a), body == null ? null : body.ids())); }

  @GetMapping("/preferences")
  public ResponseEntity<GenericApiResponse<List<PreferenceResponse>>> preferences(Authentication a) { return Responses.list(service.preferences(current.get(a)), PreferenceResponse.class); }

  @PutMapping("/preferences/{category}")
  public ResponseEntity<GenericApiResponse<List<Map<String, Object>>>> save(@PathVariable String category, @RequestBody PreferenceRequest body, Authentication a) {
    return Responses.ok(service.savePreference(current.get(a), category, body.inApp(), body.push()));
  }

  @PostMapping("/devices")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> register(@RequestBody DeviceRequest body, Authentication a) { return Responses.ok(service.registerDevice(current.get(a), body.token(), body.platform())); }

  @DeleteMapping("/devices")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> unregister(@RequestParam String token, Authentication a) { return Responses.ok(service.unregisterDevice(current.get(a), token)); }
}
