package com.logistech.fms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Value("${app.supabase.url:https://npswaigxcmyddqwdktge.supabase.co}")
  private String supabaseUrl;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        // Sin esto, Spring Security rechaza los preflight OPTIONS antes de que
        // se aplique la configuración CORS de MVC (CorsConfig) -> el navegador bloquea.
        .cors(Customizer.withDefaults())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/api/health", "/api/debug/**", "/actuator/health").permitAll()
            .requestMatchers("/api/**").authenticated()
            .anyRequest().permitAll())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
    return http.build();
  }

  /**
   * Valida los access token de Supabase Auth (ES256) contra su JWKS público.
   * Sin llamadas a Supabase por petición; la rotación de claves se recoge sola.
   */
  @Bean
  public JwtDecoder jwtDecoder() {
    NimbusJwtDecoder decoder = NimbusJwtDecoder
        .withJwkSetUri(supabaseUrl + "/auth/v1/.well-known/jwks.json")
        // Supabase firma con ES256; por defecto Spring espera RS256 y rechaza el token.
        .jwsAlgorithm(SignatureAlgorithm.ES256)
        .build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(supabaseUrl + "/auth/v1"));
    return decoder;
  }
}
