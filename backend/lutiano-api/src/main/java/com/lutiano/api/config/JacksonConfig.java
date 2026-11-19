package com.lutiano.api.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lutiano.api.utils.Responses;
import org.springframework.context.annotation.Configuration;

/** Gives {@link Responses} the application's ObjectMapper (tolerant of keys a response record does not list). */
@Configuration
public class JacksonConfig {
  public JacksonConfig(ObjectMapper mapper) {
    Responses.use(mapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false));
  }
}
