package com.lutiano.api.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * What every client (admin portal, Seller app, Customer app, seed script) relies on: the response envelope, status codes,
 * error shapes, paging, binary downloads and the staff/customer token split. These tests describe the API as it is today and
 * must keep passing, unchanged, through the whole backend restructure.
 */
class ApiContractTest extends ApiIntegrationTest {

  private static List<String> keys(JsonNode n) {
    List<String> out = new ArrayList<>();
    n.fieldNames().forEachRemaining(out::add);
    return out.stream().sorted().toList();
  }

  private void assertSuccessEnvelope(JsonNode b) {
    assertEquals(List.of("data", "success", "timestamp"), keys(b));
    assertTrue(b.path("success").asBoolean());
  }

  private void assertErrorEnvelope(JsonNode b, int status) {
    assertEquals(List.of("error", "success", "timestamp"), keys(b));
    assertFalse(b.path("success").asBoolean());
    assertEquals(List.of("code", "message", "status"), keys(b.path("error")));
    assertEquals(status, b.path("error").path("status").asInt());
  }

  @Test void healthIsWrappedLikeEverythingElse() throws Exception {
    MvcResult r = mvc.perform(get(API + "/health")).andExpect(status().isOk()).andReturn();
    assertSuccessEnvelope(body(r));
  }

  @Test void wrongPasswordIs401WithTheErrorEnvelope() throws Exception {
    MvcResult r = mvc.perform(withJson(post(API + "/auth/login"), Map.of("email", "admin@lutiano.local", "password", "nope"))).andExpect(status().isUnauthorized()).andReturn();
    assertErrorEnvelope(body(r), 401);
    assertEquals("UNAUTHORIZED", body(r).path("error").path("code").asText());
  }

  @Test void administratorsSignInInTwoStepsAndSellersInOne() throws Exception {
    JsonNode admin = body(mvc.perform(withJson(post(API + "/auth/login"), Map.of("email", "admin@lutiano.local", "password", PASSWORD))).andReturn()).path("data");
    assertTrue(admin.path("mfaRequired").asBoolean());
    assertFalse(admin.path("challenge").asText().isBlank());
    JsonNode seller = body(mvc.perform(withJson(post(API + "/auth/login"), Map.of("email", "seller@lutiano.local", "password", PASSWORD))).andReturn()).path("data");
    assertFalse(seller.path("token").asText().isBlank());
    assertEquals("SELLER", seller.path("user").path("role").asText());
  }

  @Test void missingTokenIs401AndWrongRoleIs403() throws Exception {
    assertErrorEnvelope(body(mvc.perform(get(API + "/products")).andExpect(status().isUnauthorized()).andReturn()), 401);
    String seller = staffToken("seller@lutiano.local");
    assertErrorEnvelope(body(mvc.perform(get(API + "/staff").header("Authorization", "Bearer " + seller)).andExpect(status().isForbidden()).andReturn()), 403);
  }

  @Test void clientMistakesGetTheirOwnStatusAndTheSameEnvelope() throws Exception {
    String admin = staffToken("admin@lutiano.local");
    String auth = "Bearer " + admin;
    assertErrorEnvelope(body(mvc.perform(get(API + "/products/00000000-0000-4000-8000-000000000000").header("Authorization", auth)).andExpect(status().isNotFound()).andReturn()), 404);
    assertErrorEnvelope(body(mvc.perform(get(API + "/no/such/route").header("Authorization", auth)).andExpect(status().isNotFound()).andReturn()), 404);
    assertErrorEnvelope(body(mvc.perform(patch(API + "/products").header("Authorization", auth)).andExpect(status().isMethodNotAllowed()).andReturn()), 405);
    assertErrorEnvelope(body(mvc.perform(post(API + "/products").header("Authorization", auth).contentType(MediaType.TEXT_PLAIN).content("x")).andExpect(status().isUnsupportedMediaType()).andReturn()), 415);
    assertErrorEnvelope(body(mvc.perform(post(API + "/product-categories").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content("{not json")).andExpect(status().isBadRequest()).andReturn()), 400);
    assertErrorEnvelope(body(mvc.perform(withJson(post(API + "/products"), Map.of()).header("Authorization", auth)).andExpect(status().isBadRequest()).andReturn()), 400);
  }

  @Test void listsArePagedFiveByDefaultAndCappedAt200() throws Exception {
    String auth = "Bearer " + staffToken("admin@lutiano.local");
    JsonNode page = body(mvc.perform(get(API + "/products").header("Authorization", auth)).andExpect(status().isOk()).andReturn());
    assertSuccessEnvelope(page);
    JsonNode data = page.path("data");
    assertTrue(keys(data).containsAll(List.of("items", "page", "pages", "size", "summary", "total")));
    assertEquals(5, data.path("size").asInt());
    assertEquals(1, data.path("page").asInt());
    assertEquals(200, body(mvc.perform(get(API + "/products?size=999").header("Authorization", auth)).andReturn()).path("data").path("size").asInt());
  }

  @Test void createdIs201AndDuplicatesAre409() throws Exception {
    String auth = "Bearer " + staffToken("admin@lutiano.local");
    Map<String, Object> category = Map.of("name", "Contract Category", "description", "d", "sortOrder", 1, "active", true);
    MvcResult created = mvc.perform(withJson(post(API + "/product-categories"), category).header("Authorization", auth)).andExpect(status().isCreated()).andReturn();
    assertSuccessEnvelope(body(created));
    assertTrue(keys(body(created).path("data")).containsAll(List.of("id", "name", "active", "sort_order")));
    assertErrorEnvelope(body(mvc.perform(withJson(post(API + "/product-categories"), category).header("Authorization", auth)).andExpect(status().isConflict()).andReturn()), 409);
  }

  @Test void rowsKeepTheirSnakeCaseColumnNames() throws Exception {
    String auth = "Bearer " + staffToken("admin@lutiano.local");
    JsonNode product = body(mvc.perform(get(API + "/products").header("Authorization", auth)).andReturn()).path("data").path("items").get(0);
    assertTrue(keys(product).containsAll(List.of("id", "sku", "name", "price_xaf", "active", "category", "image_url")), keys(product).toString());
    JsonNode me = body(mvc.perform(get(API + "/auth/me").header("Authorization", auth)).andReturn()).path("data");
    assertTrue(keys(me).containsAll(List.of("id", "email", "full_name", "role")), keys(me).toString());
  }

  @Test void downloadsAreRawBytesNotEnvelopes() throws Exception {
    String auth = "Bearer " + staffToken("admin@lutiano.local");
    String opId = body(mvc.perform(get(API + "/operations").header("Authorization", auth)).andReturn()).path("data").path("items").get(0).path("id").asText();
    MvcResult png = mvc.perform(get(API + "/operations/" + opId + "/qr.png").header("Authorization", auth)).andExpect(status().isOk()).andReturn();
    assertEquals("image/png", png.getResponse().getContentType());
    assertEquals((byte) 0x89, png.getResponse().getContentAsByteArray()[0]);
    MvcResult zip = mvc.perform(get(API + "/operations/qr-codes.zip").header("Authorization", auth)).andExpect(status().isOk()).andReturn();
    assertEquals("application/zip", zip.getResponse().getContentType());
    assertTrue(zip.getResponse().getHeader("Content-Disposition").contains("lutiano-qr-codes.zip"));
  }

  @Test void salesNeedAnIdempotencyKey() throws Exception {
    String auth = "Bearer " + staffToken("seller@lutiano.local");
    assertErrorEnvelope(body(mvc.perform(withJson(post(API + "/sales"), Map.of("operationId", "x", "lines", List.of(), "cashReceivedXaf", 1)).header("Authorization", auth)).andExpect(status().isBadRequest()).andReturn()), 400);
  }

  @Test void customerAndStaffTokensDoNotCrossOver() throws Exception {
    String customer = customerToken("+237690000001");
    String staff = staffToken("admin@lutiano.local");
    assertSuccessEnvelope(body(mvc.perform(get(API + "/customer/me").header("Authorization", "Bearer " + customer)).andExpect(status().isOk()).andReturn()));
    mvc.perform(get(API + "/products").header("Authorization", "Bearer " + customer)).andExpect(status().isForbidden());
    mvc.perform(get(API + "/customer/me").header("Authorization", "Bearer " + staff)).andExpect(status().isForbidden());
  }

  @Test void thePublicCatalogueNeedsNoSignIn() throws Exception {
    JsonNode page = body(mvc.perform(get(API + "/public/products")).andExpect(status().isOk()).andReturn());
    assertSuccessEnvelope(page);
    assertTrue(page.path("data").has("items"));
    assertSuccessEnvelope(body(mvc.perform(get(API + "/public/operations")).andExpect(status().isOk()).andReturn()));
  }

  @Test void deletingARuleAnswers204WithNoBody() throws Exception {
    String auth = "Bearer " + staffToken("admin@lutiano.local");
    String productId = body(mvc.perform(get(API + "/products").header("Authorization", auth)).andReturn()).path("data").path("items").get(0).path("id").asText();
    JsonNode rule = body(mvc.perform(withJson(post(API + "/commissions/rules"), Map.of("productId", productId, "sellerRole", "MOBILE_BARISTA", "basis", "PERCENT", "rate", 5, "note", "contract"))
        .header("Authorization", auth)).andExpect(status().isCreated()).andReturn()).path("data");
    MvcResult gone = mvc.perform(delete(API + "/commissions/rules/" + rule.path("id").asText()).header("Authorization", auth)).andExpect(status().isNoContent()).andReturn();
    assertEquals(0, gone.getResponse().getContentAsByteArray().length);
  }

  @Test void customerSignUpIsAccepted202() throws Exception {
    MvcResult r = mvc.perform(withJson(post(API + "/customer/auth/signup"), Map.of("fullName", "Accepted Customer", "phone", "+237690000002", "password", PASSWORD, "language", "fr", "acceptTerms", true)))
        .andExpect(status().isAccepted()).andReturn();
    assertSuccessEnvelope(body(r));
    assertEquals("sent", body(r).path("data").path("status").asText());
  }
}
