package com.lutiano.api.controller.staff;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import static com.lutiano.api.security.Roles.ADMIN;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.lutiano.api.security.CurrentUser;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.lutiano.api.service.search.SearchCandidates;
import com.lutiano.api.service.staff.SellerDocumentService;
import com.lutiano.api.service.staff.StaffService;
import com.lutiano.api.models.request.common.NoteRequest;
import com.lutiano.api.models.request.staff.PasswordResetRequest;
import com.lutiano.api.models.request.common.ReasonRequest;
import com.lutiano.api.models.request.staff.StaffRequest;
import com.lutiano.api.models.response.staff.AssignmentHistoryItemResponse;
import com.lutiano.api.models.response.staff.AssignmentResponse;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.utils.Responses;
import com.lutiano.api.models.response.staff.StaffSellerItemResponse;
import com.lutiano.api.models.response.staff.StaffItemResponse;
import com.lutiano.api.models.response.staff.StaffResponse;

/** Staff accounts, sellers, assignments and eligibility documents. Administrators only. */
@RestController
public class StaffController {
  private final StaffService service;
  private final SellerDocumentService documents;
  private final CurrentUser current;
  private final SearchCandidates candidates;

  public StaffController(StaffService service, SellerDocumentService documents, CurrentUser current, SearchCandidates candidates) {
    this.candidates = candidates;
    this.service = service; this.documents = documents; this.current = current;
  }


  @GetMapping("/staff")
  public ResponseEntity<GenericApiResponse<PageResponse<StaffItemResponse>>> list(@RequestParam(required = false) String q, @RequestParam(required = false) String role,
                                        @RequestParam(required = false) String active, PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.page(service.list(candidates.find(current.get(a), "STAFF", q), role, active, paging), StaffItemResponse.class);
  }

  @GetMapping("/sellers")
  public ResponseEntity<GenericApiResponse<PageResponse<StaffSellerItemResponse>>> sellers(@RequestParam(required = false) String q, @RequestParam(required = false) String sellerRole,
                                           @RequestParam(required = false) String active, @RequestParam(required = false) String eligible,
                                           PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.page(service.sellers(candidates.find(current.get(a), "STAFF", q), sellerRole, active, eligible, paging), StaffSellerItemResponse.class);
  }

  @GetMapping("/staff/{id:[0-9a-fA-F-]+}")
  public ResponseEntity<GenericApiResponse<StaffResponse>> detail(@PathVariable String id, Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(service.detail(id), StaffResponse.class);
  }

  @GetMapping("/staff/{id:[0-9a-fA-F-]+}/assignment-history")
  public ResponseEntity<GenericApiResponse<PageResponse<AssignmentHistoryItemResponse>>> assignmentHistory(@PathVariable String id, PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.page(service.assignmentHistory(id, paging), AssignmentHistoryItemResponse.class);
  }

  @PostMapping("/staff")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> create(@RequestBody StaffRequest body, Authentication a) {
    return Responses.created(service.create(current.require(a, ADMIN), body));
  }

  @PutMapping("/staff/{id}")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> update(@PathVariable String id, @RequestBody StaffRequest body, Authentication a) {
    return Responses.ok(service.update(current.require(a, ADMIN), id, body));
  }

  @PostMapping("/staff/{id}/deactivate")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> deactivate(@PathVariable String id, @RequestBody ReasonRequest body, Authentication a) {
    service.deactivate(current.require(a, ADMIN), id, body.reason());
    return Responses.ok(Map.of("status", "inactive"));
  }

  @PostMapping("/staff/{id}/activate")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> activate(@PathVariable String id, Authentication a) {
    service.reactivate(current.require(a, ADMIN), id);
    return Responses.ok(Map.of("status", "active"));
  }

  @PostMapping("/staff/{id:[0-9a-fA-F-]+}/image")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> uploadImage(@PathVariable String id, @RequestParam("file") MultipartFile file, Authentication a) throws Exception {
    return Responses.ok(Map.of("imageUrl", service.uploadImage(current.require(a, ADMIN), id, file)));
  }

  @PostMapping("/staff/{id:[0-9a-fA-F-]+}/mfa/reset")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> resetMfa(@PathVariable String id, Authentication a) {
    service.resetMfa(current.require(a, ADMIN), id);
    return Responses.ok(Map.of("status", "reset"));
  }

  @PostMapping("/staff/{id}/revoke-sessions")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> revokeSessions(@PathVariable String id, Authentication a) {
    service.revokeSessions(current.require(a, ADMIN), id);
    return Responses.ok(Map.of("status", "signed-out"));
  }

  @PostMapping("/staff/{id}/password")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> resetPassword(@PathVariable String id, @RequestBody PasswordResetRequest body, Authentication a) {
    service.resetPassword(current.require(a, ADMIN), id, body.password());
    return Responses.ok(Map.of("status", "reset"));
  }

  @PostMapping("/staff/{sellerId}/operations/{operationId}")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> assign(@PathVariable String sellerId, @PathVariable String operationId, Authentication a) {
    service.assign(current.require(a, ADMIN), sellerId, operationId);
    return Responses.ok(Map.of("status", "assigned"));
  }

  @DeleteMapping("/staff/{sellerId}/operations/{operationId}")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> unassign(@PathVariable String sellerId, @PathVariable String operationId, Authentication a) {
    service.unassign(current.require(a, ADMIN), sellerId, operationId);
    return Responses.ok(Map.of("status", "unassigned"));
  }

  @GetMapping("/staff/assignments")
  public ResponseEntity<GenericApiResponse<List<AssignmentResponse>>> assignments(Authentication a) {
    current.require(a, ADMIN);
    return Responses.list(service.assignments(), AssignmentResponse.class);
  }

  // ----- eligibility documents -----

  @PostMapping("/staff/{id}/documents")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> uploadDocument(@PathVariable String id, @RequestParam("type") String type,
                                            @RequestParam(value = "expiresOn", required = false) LocalDate expiresOn,
                                            @RequestParam(value = "note", required = false) String note,
                                            @RequestParam("file") MultipartFile file, Authentication a) throws Exception {
    var actor = current.require(a, ADMIN);
    service.get(id);
    return Responses.created(documents.upload(actor, id, type, expiresOn, note, file));
  }

  @PostMapping("/staff/{id}/documents/{documentId}/verify")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> verify(@PathVariable String id, @PathVariable String documentId, @RequestBody(required = false) NoteRequest body, Authentication a) {
    return Responses.ok(documents.review(current.require(a, ADMIN), id, documentId, true, body == null ? null : body.note()));
  }

  @PostMapping("/staff/{id}/documents/{documentId}/reject")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> reject(@PathVariable String id, @PathVariable String documentId, @RequestBody NoteRequest body, Authentication a) {
    return Responses.ok(documents.review(current.require(a, ADMIN), id, documentId, false, body.note()));
  }

  @GetMapping("/staff/{id}/documents/{documentId}/file")
  public ResponseEntity<byte[]> download(@PathVariable String id, @PathVariable String documentId, Authentication a) throws Exception {
    SellerDocumentService.DocumentFile f = documents.download(current.require(a, ADMIN), id, documentId);
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.contentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(f.fileName()).build().toString())
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(f.bytes());
  }
}
