package com.lutiano.api.controller.reference;

import java.util.List;
import java.util.Map;
import com.lutiano.api.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.lutiano.api.service.reference.CameroonRegions;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.reference.RegionResponse;
import com.lutiano.api.utils.Responses;

/** Shared reference data for forms. */
@RestController
public class ReferenceController {
  private final CurrentUser current;

  public ReferenceController(CurrentUser current) { this.current = current; }

  public record Region(String name, List<String> cities) {}

  @GetMapping("/reference/regions")
  public ResponseEntity<GenericApiResponse<List<RegionResponse>>> regions(Authentication a) {
    current.get(a);
    return Responses.list(CameroonRegions.CITIES_BY_REGION.entrySet().stream().map((Map.Entry<String, List<String>> e) -> new Region(e.getKey(), e.getValue())).toList(), RegionResponse.class);
  }
}
