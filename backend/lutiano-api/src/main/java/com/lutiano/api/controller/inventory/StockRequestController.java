package com.lutiano.api.controller.inventory;

import static com.lutiano.api.security.Roles.SELLER;
import static com.lutiano.api.security.Roles.STAFF;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.security.CurrentUser;
import com.lutiano.api.security.Roles;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.inventory.StockRequestService;
import com.lutiano.api.models.request.inventory.CreateStockRequest;
import com.lutiano.api.models.request.common.NoteRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.inventory.MineItemResponse;
import com.lutiano.api.utils.Responses;

/** Stock requests: several products for one sales location, decided as a whole. */
@RestController
@RequestMapping("/inventory/requests")
public class StockRequestController {
  private final StockRequestService service;
  private final CurrentUser current;

  public StockRequestController(StockRequestService service, CurrentUser current) { this.service = service; this.current = current; }


  /** A seller whose role may request stock (Mobile Barista, Market Sales Agent) for their own operation, or staff for any. */
  @PostMapping
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> create(@RequestBody CreateStockRequest body, Authentication a) {
    return Responses.created(service.create(current.require(a, Roles.ADMIN, Roles.INVENTORY_MANAGER, SELLER),
        new StockRequestService.Input(body.operationId(), body.items(), body.neededBy(), body.reason(), body.idempotencyKey())));
  }

  /** Seller view: my requests at one operation (status and reference filters), with counts per status in the page summary. */
  @GetMapping("/mine")
  public ResponseEntity<GenericApiResponse<PageResponse<MineItemResponse>>> mine(@RequestParam String operationId, @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String reference, PageRequest paging, Authentication a) {
    return Responses.page(service.mine(current.require(a, SELLER), operationId, status, reference, paging), MineItemResponse.class);
  }

  /** Staff view: every request with its products. */
  @GetMapping
  public ResponseEntity<GenericApiResponse<PageResponse<MineItemResponse>>> list(@RequestParam(required = false) String status, @RequestParam(required = false) String operationId,
                                        @RequestParam(required = false) String reference, PageRequest paging, Authentication a) {
    current.require(a, STAFF);
    return Responses.page(service.search(status, operationId, reference, paging), MineItemResponse.class);
  }

  @GetMapping("/{id}")
  public ResponseEntity<GenericApiResponse<MineItemResponse>> get(@PathVariable String id, Authentication a) {
    return Responses.ok(service.detail(current.require(a, Roles.ADMIN, Roles.INVENTORY_MANAGER, SELLER), id), MineItemResponse.class);
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
}
