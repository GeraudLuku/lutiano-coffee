package com.lutiano.api.controller.notification;

import static com.lutiano.api.security.Roles.ADMIN;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.security.CurrentUser;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.notification.AnnouncementService;
import com.lutiano.api.models.request.notification.AnnouncementRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.notification.AnnouncementItemResponse;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.utils.Responses;

/** Announcements to sellers. Administrators only. */
@RestController
@RequestMapping("/announcements")
public class AnnouncementController {
  private final AnnouncementService service;
  private final CurrentUser current;

  public AnnouncementController(AnnouncementService service, CurrentUser current) { this.service = service; this.current = current; }

  @GetMapping
  public ResponseEntity<GenericApiResponse<PageResponse<AnnouncementItemResponse>>> list(PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.page(service.list(paging), AnnouncementItemResponse.class);
  }

  @PostMapping
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> send(@RequestBody AnnouncementRequest body, Authentication a) {
    return Responses.created(service.send(current.require(a, ADMIN), body));
  }
}
