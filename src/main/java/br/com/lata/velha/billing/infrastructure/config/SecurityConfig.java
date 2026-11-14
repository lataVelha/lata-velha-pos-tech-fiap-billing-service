package br.com.lata.velha.billing.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String USER = "USER";
    private static final String ADMIN = "ADMIN";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    JwtAuthenticationConverter jwtAuthConverter) throws Exception {
        http
                // API stateless com JWT (Bearer): não há sessão/cookie a proteger,
                // então o CSRF não se aplica. // NOSONAR (java:S4502)
                .csrf(AbstractHttpConfigurer::disable) // NOSONAR
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/pagamentos/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/orcamentos/*/aprovar").hasAnyRole(USER, ADMIN)
                        .requestMatchers(HttpMethod.POST, "/orcamentos/*/reprovar").hasAnyRole(USER, ADMIN)
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
                );
        return http.build();
    }
}
