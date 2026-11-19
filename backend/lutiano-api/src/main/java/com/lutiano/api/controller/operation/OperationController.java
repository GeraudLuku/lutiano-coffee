package com.lutiano.api.controller.operation;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import static com.lutiano.api.security.Roles.ADMIN;

import java.util.List;
import java.util.Map;
import com.lutiano.api.security.CurrentUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.lutiano.api.service.operation.OperationService;
import com.lutiano.api.models.request.operation.OperatingHoursRequest;
import com.lutiano.api.models.request.operation.OperationRequest;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.operation.OperationV2Response;
import com.lutiano.api.models.response.operation.QrResponse;
import com.lutiano.api.utils.Responses;

/** Sales locations ("operations") and their permanent QR codes. */
@RestController
@RequestMapping("/operations")
public class OperationController {
  private final OperationService service;
  private final CurrentUser current;

  public OperationController(OperationService service, CurrentUser current) { this.service = service; this.current = current; }

  @GetMapping
  public ResponseEntity<GenericApiResponse<PageResponse<Map<String, Object>>>> list(@RequestParam(required = false) String q, @RequestParam(required = false) String type,
                                        @RequestParam(required = false) String active, PageRequest paging, Authentication a) {
    return Responses.ok(service.list(current.get(a), q, type, active, paging));
  }

  @GetMapping("/{id}")
  public ResponseEntity<GenericApiResponse<OperationV2Response>> detail(@PathVariable String id, Authentication a) { return Responses.ok(service.detail(current.get(a), id), OperationV2Response.class); }

  @PostMapping
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> create(@RequestBody OperationRequest body, Authentication a) {
    return Responses.created(service.create(current.require(a, ADMIN), body));
  }

  @PutMapping("/{id}")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> update(@PathVariable String id, @RequestBody OperationRequest body, Authentication a) {
    return Responses.ok(service.update(current.require(a, ADMIN), id, body));
  }

  /** Replaces the opening hours (administrators): [{weekday 1-7, opens "HH:mm", closes "HH:mm"}]; an empty list means always open. */
  @PutMapping("/{id}/hours")
  public ResponseEntity<GenericApiResponse<List<Map<String, Object>>>> hours(@PathVariable String id, @RequestBody List<OperatingHoursRequest> body, Authentication a) {
    return Responses.ok(service.setHours(current.require(a, ADMIN), id, body));
  }

  @PostMapping("/{id}/deactivate")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> deactivate(@PathVariable String id, Authentication a) {
    service.setActive(current.require(a, ADMIN), id, false);
    return Responses.ok(Map.of("status", "inactive"));
  }

  @PostMapping("/{id}/activate")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> activate(@PathVariable String id, Authentication a) {
    service.setActive(current.require(a, ADMIN), id, true);
    return Responses.ok(Map.of("status", "active"));
  }

  @GetMapping("/{id}/qr")
  public ResponseEntity<GenericApiResponse<QrResponse>> qr(@PathVariable String id, Authentication a) { return Responses.ok(service.qrInfo(current.get(a), id), QrResponse.class); }

  @PostMapping("/{id:[0-9a-fA-F-]+}/image")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> uploadImage(@PathVariable String id, @RequestParam("file") MultipartFile file, Authentication a) throws Exception {
    return Responses.ok(Map.of("imageUrl", service.uploadImage(current.require(a, ADMIN), id, file)));
  }

  /** All QR codes (optionally of one type or status) as a zip of PNGs, for printing. */
  @GetMapping(value = "/qr-codes.zip", produces = "application/zip")
  public ResponseEntity<byte[]> qrZip(@RequestParam(required = false) String type, @RequestParam(required = false) String active, Authentication a) {
    current.require(a, ADMIN);
    return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=lutiano-qr-codes.zip").body(service.qrZip(type, active));
  }

  @GetMapping(value = "/{id}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<byte[]> qrImage(@PathVariable String id, Authentication a) {
    OperationService.QrImage image = service.qrImage(current.get(a), id);
    return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + image.code() + "-qr.png")
        .body(image.png());
  }
}
