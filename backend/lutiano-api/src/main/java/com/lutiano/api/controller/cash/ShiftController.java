package com.lutiano.api.controller.cash;

import static com.lutiano.api.security.Roles.SELLER;

import java.util.List;
import java.util.Map;
import com.lutiano.api.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.cash.ShiftService;
import com.lutiano.api.models.request.cash.CloseShiftRequest;
import com.lutiano.api.models.request.cash.OpenShiftRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.cash.OpenResponse;
import com.lutiano.api.utils.Responses;

@RestController
@RequestMapping("/shifts")
public class ShiftController {
  private final ShiftService service;
  private final CurrentUser current;

  public ShiftController(ShiftService service, CurrentUser current) { this.service = service; this.current = current; }


  @PostMapping("/open")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> open(@RequestBody(required = false) OpenShiftRequest body, Authentication a) {
    return Responses.ok(service.open(current.require(a, SELLER), body == null ? null : body.openingFloat()));
  }

  @PostMapping("/close")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> close(@RequestBody CloseShiftRequest body, Authentication a) {
    return Responses.ok(service.close(current.require(a, SELLER), body.countedCash(), body.varianceReason()));
  }

  @GetMapping("/open")
  public ResponseEntity<GenericApiResponse<List<OpenResponse>>> openShifts(Authentication a) { return Responses.list(service.openShifts(current.get(a)), OpenResponse.class); }
}
