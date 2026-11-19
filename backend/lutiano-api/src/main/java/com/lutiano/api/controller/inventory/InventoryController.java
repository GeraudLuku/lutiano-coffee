package com.lutiano.api.controller.inventory;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import static com.lutiano.api.security.Roles.STAFF;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.lutiano.api.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.inventory.InventoryService;
import com.lutiano.api.models.request.inventory.ReceiptRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.inventory.ApprovalResponse;
import com.lutiano.api.models.response.inventory.CentralResponse;
import com.lutiano.api.models.response.inventory.DiscrepancyItemResponse;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.inventory.InventoryLedgerItemResponse;
import com.lutiano.api.models.response.inventory.MovementItemResponse;
import com.lutiano.api.models.response.inventory.ReceiptItemResponse;
import com.lutiano.api.models.response.inventory.ReorderResponse;
import com.lutiano.api.utils.Responses;
import com.lutiano.api.models.response.inventory.InventorySummaryResponse;

@RestController
@RequestMapping("/inventory")
public class InventoryController {
  private final InventoryService service;
  private final CurrentUser current;

  public InventoryController(InventoryService service, CurrentUser current) { this.service = service; this.current = current; }

  @GetMapping
  public ResponseEntity<GenericApiResponse<PageResponse<Map<String, Object>>>> balances(@RequestParam(required = false) String operationId, @RequestParam(required = false) String q,
                                            @RequestParam(required = false) String category, @RequestParam(required = false) String status,
                                            @RequestParam(required = false) List<String> ids, @RequestParam(required = false) String sort,
                                            PageRequest paging, Authentication a) {
    return Responses.ok(service.balances(current.get(a), operationId, q, category, status, ids, sort, paging));
  }

  /** In/out history at one operation (Seller app). */
  @GetMapping("/movements")
  public ResponseEntity<GenericApiResponse<PageResponse<MovementItemResponse>>> movements(@RequestParam String operationId, @RequestParam(required = false) String productId,
                                             @RequestParam(required = false) String direction,
                                             @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                                             PageRequest paging, Authentication a) {
    return Responses.page(service.operationMovements(current.get(a), operationId, productId, direction, from, to, paging), MovementItemResponse.class);
  }

  /** Units received and consumed at one operation over a period, and the top consumed products (Seller app). */
  @GetMapping("/summary")
  public ResponseEntity<GenericApiResponse<InventorySummaryResponse>> summary(@RequestParam String operationId, @RequestParam(required = false) String productId,
                                     @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to, Authentication a) {
    return Responses.ok(service.operationSummary(current.get(a), operationId, productId, from, to), InventorySummaryResponse.class);
  }

  /** Full stock ledger (staff). location: "CENTRAL", an operation id, or omitted for all. */
  @GetMapping("/ledger")
  public ResponseEntity<GenericApiResponse<PageResponse<InventoryLedgerItemResponse>>> ledger(@RequestParam(required = false) String location, @RequestParam(required = false) String productId,
                                          @RequestParam(required = false) String type,
                                          @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                                          PageRequest paging, Authentication a) {
    current.require(a, STAFF);
    return Responses.page(service.ledger(location, productId, type, from, to, paging), InventoryLedgerItemResponse.class);
  }

  @GetMapping("/central")
  public ResponseEntity<GenericApiResponse<List<CentralResponse>>> central(Authentication a) {
    current.require(a, STAFF);
    return Responses.list(service.central(), CentralResponse.class);
  }

  @GetMapping("/receipts")
  public ResponseEntity<GenericApiResponse<PageResponse<ReceiptItemResponse>>> receipts(PageRequest paging, Authentication a) {
    current.require(a, STAFF);
    return Responses.page(service.receipts(paging), ReceiptItemResponse.class);
  }

  @PostMapping("/receipts")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> recordReceipt(@RequestBody ReceiptRequest body, Authentication a) {
    String id = service.recordReceipt(current.require(a, STAFF), body);
    return Responses.created(Map.of("id", id, "received", body.quantity()));
  }

  @GetMapping("/discrepancies")
  public ResponseEntity<GenericApiResponse<PageResponse<DiscrepancyItemResponse>>> discrepancies(PageRequest paging, Authentication a) {
    current.require(a, STAFF);
    return Responses.page(service.discrepancies(paging), DiscrepancyItemResponse.class);
  }

  @GetMapping("/reorder")
  public ResponseEntity<GenericApiResponse<List<ReorderResponse>>> reorder(Authentication a) {
    current.require(a, STAFF);
    return Responses.list(service.reorder(), ReorderResponse.class);
  }

  @GetMapping("/approvals")
  public ResponseEntity<GenericApiResponse<ApprovalResponse>> approvals(Authentication a) {
    current.require(a, STAFF);
    return Responses.ok(service.approvals(), ApprovalResponse.class);
  }
}
