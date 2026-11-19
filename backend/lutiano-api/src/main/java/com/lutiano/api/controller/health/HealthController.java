package com.lutiano.api.controller.health;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.health.HealthResponse;
import com.lutiano.api.utils.Responses;

@RestController
public class HealthController {
  @GetMapping("/health")
  public ResponseEntity<GenericApiResponse<HealthResponse>> health() { return Responses.ok(Map.of("status", "ok", "service", "lutiano-api"), HealthResponse.class); }
}
