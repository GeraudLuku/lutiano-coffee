package com.lutiano.api;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Settings classes in config/ ({@code @ConfigurationProperties}) are picked up by the scan; schedulers are enabled in config/SchedulingConfig. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LutianoApi {
  public static void main(String[] args) {
    // Timestamps are stored and exchanged in UTC; business periods use Africa/Douala (see utils.Periods).
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SpringApplication.run(LutianoApi.class, args);
  }
}
