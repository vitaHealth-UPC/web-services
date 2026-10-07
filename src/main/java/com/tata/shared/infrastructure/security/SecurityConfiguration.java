package com.tata.shared.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            BearerSessionAuthenticationFilter bearerSessionAuthenticationFilter,
            OlderAdultOwnershipFilter olderAdultOwnershipFilter,
            AccountSelfAccessFilter accountSelfAccessFilter,
            @Value("${tata.security.require-authentication:false}") boolean requireAuthentication
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers(
                            "/health",
                            "/actuator/health",
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html"
                    ).permitAll();
                    // Public identity entry points only (not /accounts/{id}/subscription).
                    authorize.requestMatchers(HttpMethod.POST, "/api/v1/accounts").permitAll();
                    authorize.requestMatchers(HttpMethod.POST, "/api/v1/accounts/verification").permitAll();
                    authorize.requestMatchers(HttpMethod.POST, "/api/v1/sessions").permitAll();
                    authorize.requestMatchers(HttpMethod.POST, "/api/v1/pin-credentials").permitAll();
                    authorize.requestMatchers(HttpMethod.POST, "/api/v1/pin-sessions").permitAll();
                    authorize.requestMatchers(HttpMethod.POST, "/api/v1/email-verification-requests").permitAll();
                    authorize.requestMatchers(HttpMethod.GET, "/api/v1/plans").permitAll();
                    if (requireAuthentication) {
                        authorize.anyRequest().authenticated();
                    } else {
                        authorize.anyRequest().permitAll();
                    }
                })
                .addFilterBefore(bearerSessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(olderAdultOwnershipFilter, BearerSessionAuthenticationFilter.class)
                .addFilterAfter(accountSelfAccessFilter, OlderAdultOwnershipFilter.class);
        return http.build();
    }
}
