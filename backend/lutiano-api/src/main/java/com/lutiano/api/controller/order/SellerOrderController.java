package com.lutiano.api.controller.order;

import static com.lutiano.api.security.Roles.SELLER;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.security.CurrentUser;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.order.OrderService;
import com.lutiano.api.models.request.order.CollectPaymentRequest;
import com.lutiano.api.models.request.common.ReasonRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.order.OrderV3Response;
import com.lutiano.api.models.response.order.OrderItemResponse;
import com.lutiano.api.utils.Responses;

/** Customer orders at the sales points a seller works at. */
@RestController
@RequestMapping("/seller/orders")
public class SellerOrderController {
  private static final String ID = "/{id:[0-9a-fA-F-]{36}}";
  private final OrderService service;
  private final CurrentUser current;

  public SellerOrderController(OrderService service, CurrentUser current) { this.service = service; this.current = current; }


  /** group: PENDING, COMPLETED or CANCELLED; status filters one status. Orders still to handle come first; the summary counts placed, accepted and ready. */
  @GetMapping
  public ResponseEntity<GenericApiResponse<PageResponse<OrderItemResponse>>> list(@RequestParam(required = false) String group, @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String operationId, PageRequest paging, Authentication a) {
    return Responses.page(service.forSeller(current.require(a, SELLER), group, status, operationId, paging), OrderItemResponse.class);
  }

  @GetMapping(ID)
  public ResponseEntity<GenericApiResponse<OrderV3Response>> get(@PathVariable String id, Authentication a) { return Responses.ok(service.forSeller(current.require(a, SELLER), id), OrderV3Response.class); }

  @PostMapping(ID + "/accept")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> accept(@PathVariable String id, Authentication a) { return Responses.ok(service.accept(current.require(a, SELLER), id)); }

  @PostMapping(ID + "/reject")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> reject(@PathVariable String id, @RequestBody ReasonRequest body, Authentication a) { return Responses.ok(service.reject(current.require(a, SELLER), id, body.reason())); }

  @PostMapping(ID + "/ready")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> ready(@PathVariable String id, Authentication a) { return Responses.ok(service.ready(current.require(a, SELLER), id)); }

  /** Hand-over: records the cash sale; needs an open cash shift. The body is optional: { cashReceivedXaf }. */
  @PostMapping(ID + "/collect")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> collect(@PathVariable String id, @RequestBody(required = false) CollectPaymentRequest body, Authentication a) {
    return Responses.ok(service.collect(current.require(a, SELLER), id, body == null ? null : body.cashReceivedXaf()));
  }
}
