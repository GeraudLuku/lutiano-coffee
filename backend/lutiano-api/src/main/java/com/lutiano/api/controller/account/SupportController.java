package com.lutiano.api.controller.account;

import static com.lutiano.api.security.Roles.ADMIN;
import static com.lutiano.api.security.Roles.INVENTORY_MANAGER;
import static com.lutiano.api.security.Roles.SELLER;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.security.CurrentUser;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.lutiano.api.service.account.SupportService;
import com.lutiano.api.models.request.account.SupportReplyRequest;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.utils.Responses;
import com.lutiano.api.models.response.account.SupportItemResponse;

/** Help requests: sellers send and follow their own; administrators answer them. */
@RestController
public class SupportController {
  private final SupportService service;
  private final CurrentUser current;

  public SupportController(SupportService service, CurrentUser current) { this.service = service; this.current = current; }


  // ----- the seller's own -----

  /** Sends a request. Multipart: type, subject, description and an optional file (PDF, JPEG, PNG or WebP, up to 10 MB). */
  @PostMapping(value = "/me/support", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> create(@RequestParam String type, @RequestParam String subject, @RequestParam String description,
                                    @RequestParam(value = "file", required = false) MultipartFile file, Authentication a) throws Exception {
    return Responses.created(service.create(current.require(a, SELLER), type, subject, description, file));
  }

  @GetMapping("/me/support")
  public ResponseEntity<GenericApiResponse<PageResponse<SupportItemResponse>>> mine(@RequestParam(required = false) String status, PageRequest paging, Authentication a) {
    return Responses.page(service.mine(current.require(a, SELLER), status, paging), SupportItemResponse.class);
  }

  @GetMapping("/me/support/{id:[0-9a-fA-F-]{36}}")
  public ResponseEntity<GenericApiResponse<SupportItemResponse>> mineOne(@PathVariable String id, Authentication a) { return Responses.ok(service.get(current.require(a, SELLER), id), SupportItemResponse.class); }

  @GetMapping("/me/support/{id:[0-9a-fA-F-]{36}}/attachment")
  public ResponseEntity<byte[]> mineAttachment(@PathVariable String id, Authentication a) throws Exception {
    return file(service.attachmentOf(current.require(a, SELLER), id));
  }

  // ----- the team -----

  /** Every request, open ones first; the summary counts open, in progress and resolved. */
  @GetMapping("/support")
  public ResponseEntity<GenericApiResponse<PageResponse<SupportItemResponse>>> all(@RequestParam(required = false) String status, @RequestParam(required = false) String requester, PageRequest paging, Authentication a) {
    current.require(a, ADMIN, INVENTORY_MANAGER);
    return Responses.page(service.all(status, requester, paging), SupportItemResponse.class);
  }

  @GetMapping("/support/{id:[0-9a-fA-F-]{36}}")
  public ResponseEntity<GenericApiResponse<SupportItemResponse>> one(@PathVariable String id, Authentication a) {
    return Responses.ok(service.get(current.require(a, ADMIN, INVENTORY_MANAGER), id), SupportItemResponse.class);
  }

  @GetMapping("/support/{id:[0-9a-fA-F-]{36}}/attachment")
  public ResponseEntity<byte[]> attachment(@PathVariable String id, Authentication a) throws Exception {
    return file(service.attachmentOf(current.require(a, ADMIN, INVENTORY_MANAGER), id));
  }

  /** Writes the reply the seller will read and/or changes the status (OPEN, IN_PROGRESS, RESOLVED). */
  @PostMapping("/support/{id:[0-9a-fA-F-]{36}}/reply")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> reply(@PathVariable String id, @RequestBody SupportReplyRequest body, Authentication a) {
    return Responses.ok(service.reply(current.require(a, ADMIN, INVENTORY_MANAGER), id, body.status(), body.reply()));
  }

  private static ResponseEntity<byte[]> file(SupportService.Attachment f) {
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.contentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(f.fileName()).build().toString())
        .header(HttpHeaders.CACHE_CONTROL, "no-store").body(f.bytes());
  }
}
