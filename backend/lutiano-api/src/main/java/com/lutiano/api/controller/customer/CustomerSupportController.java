package com.lutiano.api.controller.customer;

import com.lutiano.api.service.account.SupportService;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.security.CurrentCustomer;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.utils.Responses;
import com.lutiano.api.models.response.account.SupportItemResponse;

/** A customer's help requests to the LUTIANO team, and the team's replies (the same inbox the sellers' requests go to). */
@RestController
@RequestMapping("/customer/support")
public class CustomerSupportController {
  private static final String ID = "/{id:[0-9a-fA-F-]{36}}";
  private final SupportService service;
  private final CurrentCustomer current;

  public CustomerSupportController(SupportService service, CurrentCustomer current) { this.service = service; this.current = current; }

  /** Multipart: type (QUESTION, ISSUE, IMPROVEMENT, ACCOUNT, ORDER, REWARDS), subject, description and an optional file (PDF, JPEG, PNG or WebP, up to 10 MB). */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> create(@RequestParam String type, @RequestParam String subject, @RequestParam String description,
                                    @RequestParam(value = "file", required = false) MultipartFile file, Authentication a) throws Exception {
    return Responses.created(service.create(current.get(a), type, subject, description, file));
  }

  @GetMapping
  public ResponseEntity<GenericApiResponse<PageResponse<SupportItemResponse>>> mine(@RequestParam(required = false) String status, PageRequest paging, Authentication a) { return Responses.page(service.mine(current.get(a), status, paging), SupportItemResponse.class); }

  @GetMapping(ID)
  public ResponseEntity<GenericApiResponse<SupportItemResponse>> one(@PathVariable String id, Authentication a) { return Responses.ok(service.get(current.get(a), id), SupportItemResponse.class); }

  @GetMapping(ID + "/attachment")
  public ResponseEntity<byte[]> attachment(@PathVariable String id, Authentication a) throws Exception {
    SupportService.Attachment f = service.attachmentOf(current.get(a), id);
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.contentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(f.fileName()).build().toString()).body(f.bytes());
  }
}
