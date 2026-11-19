package com.lutiano.api.controller.inventory;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import static com.lutiano.api.security.Roles.STAFF;

import java.util.Map;
import com.lutiano.api.security.CurrentUser;
import com.lutiano.api.security.Roles;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.inventory.AdjustmentService;
import com.lutiano.api.models.request.common.NoteRequest;
import com.lutiano.api.models.request.inventory.StockAdjustmentRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.inventory.InventoryAdjustmentItemResponse;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.utils.Responses;

@RestController
@RequestMapping("/inventory/adjustments")
public class AdjustmentController {
  private final AdjustmentService service;
  private final CurrentUser current;

  public AdjustmentController(AdjustmentService service, CurrentUser current) { this.service = service; this.current = current; }


  /** @param location "CENTRAL", an operation id, or omitted for all. */
  @GetMapping
  public ResponseEntity<GenericApiResponse<PageResponse<InventoryAdjustmentItemResponse>>> list(@RequestParam(required = false) String status, @RequestParam(required = false) String location,
                                        PageRequest paging, Authentication a) {
    return Responses.page(service.search(current.get(a), status, location, paging), InventoryAdjustmentItemResponse.class);
  }

  @PostMapping
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> request(@RequestBody StockAdjustmentRequest body, Authentication a) {
    return Responses.created(service.request(current.require(a, Roles.ADMIN, Roles.INVENTORY_MANAGER, Roles.SELLER),
        body.operationId(), body.productId(), body.quantityDelta(), body.reason(), body.note()));
  }

  @PostMapping("/{id}/approve")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> approve(@PathVariable String id, @RequestBody(required = false) NoteRequest body, Authentication a) {
    return Responses.ok(service.approve(current.require(a, STAFF), id, body == null ? null : body.note()));
  }

  @PostMapping("/{id}/reject")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> reject(@PathVariable String id, @RequestBody NoteRequest body, Authentication a) {
    return Responses.ok(service.reject(current.require(a, STAFF), id, body.note()));
  }

  @PostMapping("/{id}/cancel")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> cancel(@PathVariable String id, Authentication a) {
    return Responses.ok(service.cancel(current.require(a, Roles.ADMIN, Roles.INVENTORY_MANAGER, Roles.SELLER), id));
  }
}
