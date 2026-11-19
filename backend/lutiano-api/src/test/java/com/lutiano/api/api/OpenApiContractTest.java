package com.lutiano.api.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * packages/api-contracts/openapi.yaml is generated from the controllers and typed responses. This test fails when the committed file
 * is out of date; run with {@code UPDATE_OPENAPI=true ./gradlew test --tests '*OpenApiContractTest'} to rewrite it.
 */
class OpenApiContractTest extends ApiIntegrationTest {
  private static final Path FILE = Path.of("../../packages/api-contracts/openapi.yaml");

  @Test void committedSpecMatchesTheControllers() throws Exception {
    String token = staffToken("admin@lutiano.local");
    String yaml = mvc.perform(get("/v3/api-docs.yaml").header("Authorization", "Bearer " + token)).andReturn().getResponse().getContentAsString();
    assertTrue(yaml.contains("openapi:"), yaml.substring(0, Math.min(200, yaml.length())));
    assertTrue(yaml.contains("/sales"), "the spec lists the sales endpoints");
    if ("true".equals(System.getenv("UPDATE_OPENAPI"))) { Files.writeString(FILE, yaml); return; }
    assertEquals(Files.readString(FILE).strip(), yaml.strip(), "openapi.yaml is out of date: run with UPDATE_OPENAPI=true to regenerate it");
  }
}
