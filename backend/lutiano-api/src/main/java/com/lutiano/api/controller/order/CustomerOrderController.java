package com.lutiano.api.controller.order;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.service.sales.ReceiptPdfRenderer;
import com.lutiano.api.security.CurrentCustomer;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.order.OrderService;
import com.lutiano.api.models.request.order.PlaceOrderRequest;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.order.OrderV2Response;
import com.lutiano.api.models.response.order.OrderItemV2Response;
import com.lutiano.api.models.response.order.OrderReorderResponse;
import com.lutiano.api.utils.Responses;

/** A customer's own orders. */
@RestController
@RequestMapping("/customer/orders")
public class CustomerOrderController {
  private static final String ID = "/{id:[0-9a-fA-F-]{36}}";
  private final OrderService service;
  private final ReceiptPdfRenderer receipts;
  private final CurrentCustomer current;

  public CustomerOrderController(OrderService service, ReceiptPdfRenderer receipts, CurrentCustomer current) { this.service = service; this.receipts = receipts; this.current = current; }

  /** 201 for a new order, 200 when a retry with the same Idempotency-Key returns the one already placed. */
  @PostMapping
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> place(@RequestBody PlaceOrderRequest body, @RequestHeader("Idempotency-Key") String key, Authentication a) {
    OrderService.OrderResult r = service.place(current.get(a), key, body);
    return ResponseEntity.status(r.created() ? HttpStatus.CREATED : HttpStatus.OK).body(GenericApiResponse.ok(r.order()));
  }

  /** group: PENDING, COMPLETED or CANCELLED; newest first. */
  @GetMapping
  public ResponseEntity<GenericApiResponse<PageResponse<OrderItemV2Response>>> list(@RequestParam(required = false) String group, PageRequest paging, Authentication a) { return Responses.page(service.mine(current.get(a), group, paging), OrderItemV2Response.class); }

  @GetMapping(ID)
  public ResponseEntity<GenericApiResponse<OrderV2Response>> get(@PathVariable String id, Authentication a) { return Responses.ok(service.mine(current.get(a), id), OrderV2Response.class); }

  @PostMapping(ID + "/cancel")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> cancel(@PathVariable String id, Authentication a) { return Responses.ok(service.cancel(current.get(a), id)); }

  /** The lines again at today's prices, each saying whether it can still be ordered. */
  @GetMapping(ID + "/reorder")
  public ResponseEntity<GenericApiResponse<List<OrderReorderResponse>>> reorder(@PathVariable String id, Authentication a) { return Responses.list(service.reorder(current.get(a), id), OrderReorderResponse.class); }

  @GetMapping(ID + "/receipt.pdf")
  public ResponseEntity<byte[]> receipt(@PathVariable String id, Authentication a) {
    Map<String, Object> sale = service.receipt(current.get(a), id);
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=lutiano-receipt-" + sale.get("reference") + ".pdf").body(receipts.render(sale));
  }
}
