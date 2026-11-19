package com.lutiano.api.controller.customer;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.security.CurrentCustomer;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.customer.CustomerProfileService;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.utils.Responses;

/** The products a customer marked with a heart. */
@RestController
@RequestMapping("/customer/favourites")
public class CustomerFavouriteController {
  private final CustomerProfileService service;
  private final CurrentCustomer current;

  public CustomerFavouriteController(CustomerProfileService service, CurrentCustomer current) { this.service = service; this.current = current; }

  @GetMapping
  public ResponseEntity<GenericApiResponse<PageResponse<Map<String, Object>>>> list(PageRequest paging, Authentication a) { return Responses.ok(service.favourites(current.get(a), paging)); }

  @PutMapping("/{productId:[0-9a-fA-F-]{36}}")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> add(@PathVariable String productId, Authentication a) { return Responses.ok(service.addFavourite(current.get(a), productId)); }

  @DeleteMapping("/{productId:[0-9a-fA-F-]{36}}")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> remove(@PathVariable String productId, Authentication a) { return Responses.ok(service.removeFavourite(current.get(a), productId)); }
}
