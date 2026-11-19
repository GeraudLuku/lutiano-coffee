package com.lutiano.api.controller.campaign;

import static com.lutiano.api.security.Roles.SELLER;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.security.CurrentUser;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.campaign.CampaignService;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.utils.Responses;

/** The campaigns that reach a seller, with their own progress. Sellers only. */
@RestController
@RequestMapping("/me/campaigns")
public class MyCampaignsController {
  private final CampaignService service;
  private final CurrentUser current;

  public MyCampaignsController(CampaignService service, CurrentUser current) { this.service = service; this.current = current; }

  /** state: CURRENT, UPCOMING or ENDED. */
  @GetMapping
  public ResponseEntity<GenericApiResponse<PageResponse<Map<String, Object>>>> list(@RequestParam(required = false) String state, PageRequest paging, Authentication a) {
    return Responses.ok(service.mine(current.require(a, SELLER), state, paging));
  }

  @GetMapping("/{id:[0-9a-fA-F-]+}")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> get(@PathVariable String id, Authentication a) { return Responses.ok(service.mine(current.require(a, SELLER), id)); }
}
