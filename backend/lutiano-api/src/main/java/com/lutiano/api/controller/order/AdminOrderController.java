package com.lutiano.api.controller.order;

import static com.lutiano.api.security.Roles.ADMIN;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.security.CurrentUser;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.order.OrderService;
import com.lutiano.api.models.request.common.ReasonRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.order.OrderItemResponse;
import com.lutiano.api.models.response.order.OrderResponse;
import com.lutiano.api.utils.Responses;

/** All customer orders, for administrators. */
@RestController
@RequestMapping("/orders")
public class AdminOrderController {
  private static final String ID = "/{id:[0-9a-fA-F-]{36}}";
  private final OrderService service;
  private final CurrentUser current;

  public AdminOrderController(OrderService service, CurrentUser current) { this.service = service; this.current = current; }


  @GetMapping
  public ResponseEntity<GenericApiResponse<PageResponse<OrderItemResponse>>> list(@RequestParam(required = false) String group, @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String operationId, PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.page(service.all(group, status, operationId, paging), OrderItemResponse.class);
  }

  @GetMapping(ID)
  public ResponseEntity<GenericApiResponse<OrderResponse>> get(@PathVariable String id, Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(service.get(id), OrderResponse.class);
  }

  @PostMapping(ID + "/cancel")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> cancel(@PathVariable String id, @RequestBody ReasonRequest body, Authentication a) { return Responses.ok(service.adminCancel(current.require(a, ADMIN), id, body.reason())); }
}
