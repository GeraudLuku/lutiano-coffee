package com.lutiano.api.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import java.util.Map;

/**
 * Boots the whole API (real security filters, real Flyway migrations, real MariaDB and Redis in containers, Elasticsearch off)
 * and talks to it through MockMvc, so tests see exactly what an HTTP client sees: status, headers and the JSON envelope.
 * The containers are shared by every subclass for the whole test run.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class ApiIntegrationTest {
  static final MariaDBContainer<?> MARIA = new MariaDBContainer<>("mariadb:11.4");
  static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

  static {
    MARIA.start();
    REDIS.start();
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", MARIA::getJdbcUrl);
    r.add("spring.datasource.username", MARIA::getUsername);
    r.add("spring.datasource.password", MARIA::getPassword);
    r.add("spring.data.redis.host", REDIS::getHost);
    r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    r.add("lutiano.search.indexing-enabled", () -> "false");   // search is covered by SearchLifecycleTest
  }

  /** MockMvc has no servlet context path, so paths here are the ones after /api/v1. */
  protected static final String API = "";
  protected static final String PASSWORD = "Coffee123!";
  protected static final String DEV_CODE = "123456";

  @Autowired protected MockMvc mvc;
  @Autowired protected ObjectMapper json;

  protected JsonNode body(MvcResult r) throws Exception { return json.readTree(r.getResponse().getContentAsByteArray()); }

  protected MockHttpServletRequestBuilder withJson(MockHttpServletRequestBuilder b, Object payload) throws Exception {
    return b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(payload));
  }

  /** Signs a staff member in (administrators answer the second step with the development code). */
  protected String staffToken(String email) throws Exception {
    JsonNode d = body(mvc.perform(withJson(post(API + "/auth/login"), java.util.Map.of("email", email, "password", PASSWORD))).andReturn()).path("data");
    if (d.path("mfaRequired").asBoolean(false)) {
      d = body(mvc.perform(withJson(post(API + "/auth/mfa/verify"), java.util.Map.of("challenge", d.path("challenge").asText(), "code", DEV_CODE))).andReturn()).path("data");
    }
    return d.path("token").asText();
  }

  /** A customer account created through sign-up (the SMS code is the development code). */
  protected String customerToken(String phone) throws Exception {
    mvc.perform(withJson(post(API + "/customer/auth/signup"), java.util.Map.of("fullName", "Contract Customer", "phone", phone, "password", PASSWORD, "language", "fr", "acceptTerms", true)));
    return body(mvc.perform(withJson(post(API + "/customer/auth/signup/verify"), java.util.Map.of("phone", phone, "code", DEV_CODE))).andReturn()).path("data").path("token").asText();
  }
}
