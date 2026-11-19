package com.lutiano.api.controller.campaign;

import static com.lutiano.api.security.Roles.ADMIN;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.security.CurrentUser;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.campaign.CampaignService;
import com.lutiano.api.models.request.campaign.CampaignRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.utils.Responses;

/** Sales campaigns and their tiered bonuses. Administrators only. */
@RestController
@RequestMapping("/campaigns")
public class CampaignController {
  private static final String ID = "/{id:[0-9a-fA-F-]+}";
  private final CampaignService service;
  private final CurrentUser current;

  public CampaignController(CampaignService service, CurrentUser current) { this.service = service; this.current = current; }

  @PostMapping
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> create(@RequestBody CampaignRequest body, Authentication a) { return Responses.created(service.create(current.require(a, ADMIN), body)); }

  /** state: CURRENT, UPCOMING or ENDED. */
  @GetMapping
  public ResponseEntity<GenericApiResponse<PageResponse<Map<String, Object>>>> list(@RequestParam(required = false) String state, PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(service.list(state, paging));
  }

  @GetMapping(ID)
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> get(@PathVariable String id, Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(service.detail(id));
  }

  /** Every seller the campaign reaches with their net sales and the bonus they would get now. */
  @GetMapping(ID + "/standings")
  public ResponseEntity<GenericApiResponse<PageResponse<Map<String, Object>>>> standings(@PathVariable String id, PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(service.standings(id, paging));
  }

  @PostMapping(ID + "/cancel")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> cancel(@PathVariable String id, Authentication a) { return Responses.ok(service.cancel(current.require(a, ADMIN), id)); }

  /** Pays the bonuses after the last day; works once. */
  @PostMapping(ID + "/settle")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> settle(@PathVariable String id, Authentication a) { return Responses.ok(service.settle(current.require(a, ADMIN), id)); }
}
