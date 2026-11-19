package com.lutiano.api.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lutiano.api.models.response.GenericApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.lutiano.api.security.Roles;

@Configuration
public class SecurityConfig {

  @Bean
  UserDetailsService users(JdbcTemplate jdbc) {
    return email -> jdbc.query(
            "select email,password_hash,role,active from staff_user where email=?",
            (rs, n) -> User.withUsername(rs.getString(1)).password(rs.getString(2))
                .roles(rs.getString(3)).disabled(!rs.getBoolean(4)).build(),
            email.trim().toLowerCase())
        .stream().findFirst()
        .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(users);
    provider.setPasswordEncoder(encoder);
    return new ProviderManager(provider);
  }


  /** Security errors use the same envelope as every other response. */
  private void writeError(HttpServletResponse res, HttpStatus status, String message) throws IOException {
    res.setStatus(status.value());
    res.setContentType("application/json");
    res.getWriter().write(errorJson.writeValueAsString(GenericApiResponse.error(status, message)));
  }

  private final ObjectMapper errorJson =
      new ObjectMapper().registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
    roles.setAuthoritiesClaimName("roles");
    roles.setAuthorityPrefix("ROLE_");
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(roles);

    return http
        .cors(Customizer.withDefaults())
        .csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            // health, info and prometheus: served on the separate management port (MANAGEMENT_PORT), not published publicly
            .requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
            .requestMatchers("/health", "/auth/login", "/auth/mfa/**", "/auth/password/forgot", "/auth/password/reset", "/media/**", "/error", "/catalogue/search").permitAll()
            // the Customer app: open sign-up and sign-in and the public menu; everything else of a customer needs a customer token
            .requestMatchers("/public/**", "/customer/auth/**").permitAll()
            .requestMatchers("/customer/**").hasRole(Roles.CUSTOMER)
            // a customer token never works on a staff endpoint, whatever the endpoint checks
            .anyRequest().access((auth, ctx) -> new AuthorizationDecision(isStaffToken(auth.get()))))
        .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter))
            .authenticationEntryPoint((req, res, e) -> writeError(res, HttpStatus.UNAUTHORIZED, "Your session has expired. Sign in again.")))
        .exceptionHandling(x -> x
            .authenticationEntryPoint((req, res, e) -> writeError(res, HttpStatus.UNAUTHORIZED, "Sign in to continue."))
            .accessDeniedHandler((req, res, e) -> writeError(res, HttpStatus.FORBIDDEN, "You do not have permission to do this.")))
        .build();
  }

  /** A signed-in caller who is not a customer: the only kind that may use staff endpoints. */
  static boolean isStaffToken(Authentication a) {
    return a != null && a.isAuthenticated() && !(a instanceof AnonymousAuthenticationToken)
        && a.getAuthorities().stream().noneMatch(g -> g.getAuthority().equals("ROLE_" + Roles.CUSTOMER));
  }
}
