package com.lutiano.api.controller.inventory;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import static com.lutiano.api.security.Roles.SELLER;
import static com.lutiano.api.security.Roles.STAFF;

import java.util.Map;
import com.lutiano.api.security.CurrentUser;
import com.lutiano.api.security.Roles;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.inventory.TransferService;
import com.lutiano.api.models.request.inventory.ConfirmReceiptRequest;
import com.lutiano.api.models.request.common.NoteRequest;
import com.lutiano.api.models.request.inventory.TransferRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.inventory.AllocationItemResponse;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.inventory.InventoryMineItemResponse;
import com.lutiano.api.utils.Responses;

/** Stock transfers ("allocations") from central stock to sales operations. */
@RestController
@RequestMapping("/inventory/allocations")
public class TransferController {
  private final TransferService service;
  private final CurrentUser current;

  public TransferController(TransferService service, CurrentUser current) { this.service = service; this.current = current; }


  @GetMapping
  public ResponseEntity<GenericApiResponse<PageResponse<AllocationItemResponse>>> list(@RequestParam(required = false) String status, @RequestParam(required = false) String operationId,
                                        @RequestParam(required = false) String q, PageRequest paging, Authentication a) {
    current.require(a, STAFF);
    return Responses.page(service.search(status, operationId, q, paging), AllocationItemResponse.class);
  }

  /** Staff: a single-product transfer awaiting approval (sellers ask for stock with a stock request). */
  @PostMapping
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> request(@RequestBody TransferRequest body, Authentication a) {
    return Responses.created(service.request(current.require(a, STAFF), body.operationId(), body.productId(), body.quantity(), body.note()));
  }

  @PostMapping("/{id}/approve")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> approve(@PathVariable String id, @RequestBody(required = false) NoteRequest body, Authentication a) {
    return Responses.ok(service.approve(current.require(a, STAFF), id, body == null ? null : body.note()));
  }

  @PostMapping("/{id}/reject")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> reject(@PathVariable String id, @RequestBody NoteRequest body, Authentication a) {
    return Responses.ok(service.reject(current.require(a, STAFF), id, body.note()));
  }

  @PostMapping("/{id}/dispatch")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> dispatch(@PathVariable String id, Authentication a) {
    return Responses.ok(service.dispatch(current.require(a, STAFF), id));
  }

  @PostMapping("/{id}/cancel")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> cancel(@PathVariable String id, @RequestBody NoteRequest body, Authentication a) {
    return Responses.ok(service.cancel(current.require(a, Roles.ADMIN, Roles.INVENTORY_MANAGER, SELLER), id, body.note()));
  }

  /** Seller view: transfers to their operations (default status IN_TRANSIT). */
  @GetMapping("/mine")
  public ResponseEntity<GenericApiResponse<PageResponse<InventoryMineItemResponse>>> mine(@RequestParam(required = false) String operationId, @RequestParam(required = false) String status,
                                        PageRequest paging, Authentication a) {
    return Responses.page(service.forSeller(current.require(a, SELLER), operationId, status, paging), InventoryMineItemResponse.class);
  }

  @PostMapping("/confirm")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> confirm(@RequestBody ConfirmReceiptRequest body, Authentication a) {
    boolean discrepancy = service.confirmReceipt(current.require(a, Roles.ADMIN, Roles.INVENTORY_MANAGER, SELLER), body.allocationId(), body.quantity(), body.note());
    return Responses.ok(Map.of("status", "RECEIVED", "discrepancy", discrepancy));
  }
}
