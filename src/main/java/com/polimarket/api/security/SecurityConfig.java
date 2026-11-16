package com.polimarket.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cadena de seguridad unica para todos los perfiles: API stateless protegida con JWT (RS256).
 * <p>
 * Cada peticion con {@code Authorization: Bearer <jwt>} se valida (firma, exp, iss, aud) y el claim
 * {@code roles} se convierte en autoridades {@code ROLE_*}. La matriz de autorizacion esta centralizada aqui
 * y todo lo no listado se deniega. La autorizacion comercial de Recursos Humanos sigue en la capa service.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@Import(JwtConfig.class)
public class SecurityConfig {

    static final String ADMIN = "ADMIN";
    static final String VENDEDOR = "VENDEDOR";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(ProblemDetailSecurityHandlers.entryPoint())
                        .accessDeniedHandler(ProblemDetailSecurityHandlers.accessDeniedHandler()))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(ProblemDetailSecurityHandlers.entryPoint())
                        .accessDeniedHandler(ProblemDetailSecurityHandlers.accessDeniedHandler()))
                .authorizeHttpRequests(auth -> auth
                        // Publicos
                        .requestMatchers(HttpMethod.GET, "/api/v1/hola").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                        // Cualquier usuario autenticado
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").hasAnyRole(ADMIN, VENDEDOR)
                        // Operacion comercial: VENDEDOR y ADMIN
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/clientes/**", "/api/v1/productos/**", "/api/v1/inventarios/**",
                                "/api/v1/ventas/**", "/api/v1/entregas/**").hasAnyRole(ADMIN, VENDEDOR)
                        .requestMatchers(HttpMethod.POST, "/api/v1/ventas").hasAnyRole(ADMIN, VENDEDOR)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/ventas/*/cancelacion").hasAnyRole(ADMIN, VENDEDOR)
                        // Administracion: solo ADMIN
                        .requestMatchers("/api/v1/**").hasRole(ADMIN)
                        .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // BCrypt por defecto, con prefijo {bcrypt} para poder migrar de algoritmo en el futuro
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }
}
