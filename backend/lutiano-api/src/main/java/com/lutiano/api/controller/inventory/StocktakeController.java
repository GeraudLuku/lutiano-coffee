package com.lutiano.api.controller.inventory;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import static com.lutiano.api.security.Roles.STAFF;

import java.util.Map;
import com.lutiano.api.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.inventory.StocktakeService;
import com.lutiano.api.models.request.common.NoteRequest;
import com.lutiano.api.models.request.inventory.StartStocktakeRequest;
import com.lutiano.api.models.request.inventory.StocktakeCountsRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.utils.Responses;
import com.lutiano.api.models.response.inventory.StocktakeItemResponse;
import com.lutiano.api.models.response.inventory.StocktakeResponse;

/** Stock counts (staff only). */
@RestController
@RequestMapping("/inventory/stocktakes")
public class StocktakeController {
  private final StocktakeService service;
  private final CurrentUser current;

  public StocktakeController(StocktakeService service, CurrentUser current) { this.service = service; this.current = current; }


  @GetMapping
  public ResponseEntity<GenericApiResponse<PageResponse<StocktakeItemResponse>>> list(@RequestParam(required = false) String status, @RequestParam(required = false) String location,
                                        PageRequest paging, Authentication a) {
    current.require(a, STAFF);
    return Responses.page(service.search(status, location, paging), StocktakeItemResponse.class);
  }

  @PostMapping
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> start(@RequestBody StartStocktakeRequest body, Authentication a) {
    return Responses.created(service.start(current.require(a, STAFF), body.operationId(), body.note()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<GenericApiResponse<StocktakeResponse>> detail(@PathVariable String id, Authentication a) {
    current.require(a, STAFF);
    return Responses.ok(service.detail(id), StocktakeResponse.class);
  }

  @PutMapping("/{id}/counts")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> saveCounts(@PathVariable String id, @RequestBody StocktakeCountsRequest body, Authentication a) {
    return Responses.ok(service.saveCounts(current.require(a, STAFF), id, body.counts()));
  }

  @PostMapping("/{id}/submit")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> submit(@PathVariable String id, Authentication a) { return Responses.ok(service.submit(current.require(a, STAFF), id)); }

  @PostMapping("/{id}/approve")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> approve(@PathVariable String id, @RequestBody(required = false) NoteRequest body, Authentication a) {
    return Responses.ok(service.approve(current.require(a, STAFF), id, body == null ? null : body.note()));
  }

  @PostMapping("/{id}/reject")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> reject(@PathVariable String id, @RequestBody NoteRequest body, Authentication a) {
    return Responses.ok(service.reject(current.require(a, STAFF), id, body.note()));
  }

  @PostMapping("/{id}/cancel")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> cancel(@PathVariable String id, @RequestBody NoteRequest body, Authentication a) {
    return Responses.ok(service.cancel(current.require(a, STAFF), id, body.note()));
  }
}
