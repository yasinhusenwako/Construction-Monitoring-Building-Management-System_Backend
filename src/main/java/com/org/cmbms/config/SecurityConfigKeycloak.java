package com.org.cmbms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfigKeycloak {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/actuator/health",
                    "/api/history/backfill-actors",
                    "/api/history/debug/**"
                ).permitAll()
                // Admin endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Supervisor endpoints
                .requestMatchers("/api/supervisor/**").hasAnyRole("SUPERVISOR", "ADMIN")
                // Professional endpoints
                .requestMatchers("/api/professional/**").hasRole("PROFESSIONAL")
                // Shared endpoints
                .requestMatchers("/api/projects/**", "/api/bookings/**", "/api/maintenance/**")
                    .hasAnyRole("ADMIN", "USER", "SUPERVISOR", "PROFESSIONAL")
                .requestMatchers("/api/users/**").authenticated()
                .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "SUPERVISOR")
                .requestMatchers("/api/preventive-schedules/**")
                    .hasAnyRole("ADMIN", "SUPERVISOR", "PROFESSIONAL")
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }

    @Bean
    public org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder() {
        // Use NimbusJwtDecoder with explicit JWK Set URI instead of issuer location
        // This avoids the issuer validation issue
        org.springframework.security.oauth2.jwt.NimbusJwtDecoder jwtDecoder = 
            org.springframework.security.oauth2.jwt.NimbusJwtDecoder
                .withJwkSetUri("http://localhost:8090/realms/buildms/protocol/openid-connect/certs")
                .build();
        
        // Set custom validator that only validates issuer
        jwtDecoder.setJwtValidator(
            org.springframework.security.oauth2.jwt.JwtValidators
                .createDefaultWithIssuer("http://localhost:8090/realms/buildms")
        );
        
        return jwtDecoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            "http://localhost:3000",
            "http://localhost:8090"  // Keycloak
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    /**
     * Custom converter to extract roles from Keycloak JWT token
     */
    static class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            // Extract realm roles
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            Collection<String> realmRoles = realmAccess != null 
                ? (Collection<String>) realmAccess.get("roles") 
                : List.of();
            
            // Extract resource roles (client-specific roles)
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            Collection<String> resourceRoles = List.of();
            if (resourceAccess != null) {
                Map<String, Object> clientResource = (Map<String, Object>) resourceAccess.get("buildms-backend");
                if (clientResource != null) {
                    resourceRoles = (Collection<String>) clientResource.get("roles");
                }
            }
            
            // Combine realm and resource roles
            return Stream.concat(
                realmRoles.stream(),
                resourceRoles != null ? resourceRoles.stream() : Stream.empty()
            )
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList());
        }
    }
}
