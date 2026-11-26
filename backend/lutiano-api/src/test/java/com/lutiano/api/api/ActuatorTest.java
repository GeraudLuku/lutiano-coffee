package com.lutiano.api.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * The operations endpoints live on their own management port: probes for Kubernetes, Prometheus metrics and build info, and nothing
 * else. This starts the API for real (random ports) because MockMvc cannot reach a separate management port.
 */
@AutoConfigureObservability   // tests switch metrics export off by default; here the Prometheus endpoint is what is under test
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "management.server.port=0")
class ActuatorTest extends ApiIntegrationTest {
  @LocalManagementPort int managementPort;
  @LocalServerPort int serverPort;
  private final HttpClient http = HttpClient.newHttpClient();

  private HttpResponse<String> get(int port, String path) throws Exception {
    return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
  }

  @Test void livenessIsUp() throws Exception {
    var r = get(managementPort, "/actuator/health/liveness");
    assertEquals(200, r.statusCode());
    assertTrue(r.body().contains("\"UP\""));
  }

  @Test void readinessChecksTheDatabaseAndRedisButNotSearch() throws Exception {
    var r = get(managementPort, "/actuator/health/readiness");
    assertEquals(200, r.statusCode());
    assertTrue(r.body().contains("\"db\""));
    assertTrue(r.body().contains("\"redis\""));
    assertFalse(r.body().contains("elasticsearch"), "search is optional, so it must not decide readiness");
  }

  @Test void prometheusServesMetrics() throws Exception {
    var r = get(managementPort, "/actuator/prometheus");
    assertEquals(200, r.statusCode());
    assertTrue(r.body().contains("jvm_memory_used_bytes"));
    assertTrue(r.body().contains("application=\"lutiano-api\""));
  }

  @Test void infoShowsTheBuild() throws Exception {
    var r = get(managementPort, "/actuator/info");
    assertEquals(200, r.statusCode());
    assertTrue(r.body().contains("lutiano-api"));
  }

  @Test void dangerousEndpointsAreNotAvailable() throws Exception {
    for (String path : new String[] {"env", "beans", "heapdump", "threaddump", "loggers", "mappings", "metrics"})
      assertNotEquals(200, get(managementPort, "/actuator/" + path).statusCode(), path);
  }

  @Test void nothingIsServedOnThePublicPort() throws Exception {
    for (String path : new String[] {"/api/v1/actuator/health", "/api/v1/actuator/prometheus", "/actuator/health"})
      assertNotEquals(200, get(serverPort, path).statusCode(), path);
  }

  @Test void theExistingHealthEndpointStillWorks() throws Exception {
    var r = get(serverPort, "/api/v1/health");
    assertEquals(200, r.statusCode());
    assertTrue(r.body().contains("\"ok\""));
  }
}
