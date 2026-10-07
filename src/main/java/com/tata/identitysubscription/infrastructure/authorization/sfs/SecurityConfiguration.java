package com.tata.identitysubscription.infrastructure.authorization.sfs;
import com.tata.identitysubscription.application.queryservices.SessionQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration
public class SecurityConfiguration {
 @Bean SecurityFilterChain securityFilterChain(HttpSecurity http,SessionQueryService sessions) throws Exception {
  http.csrf(c -> c.disable()).cors(Customizer.withDefaults())
   .httpBasic(c -> c.disable()).formLogin(c -> c.disable()).logout(c -> c.disable())
   .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a -> a.requestMatchers(PublicSessionEndpoints::matches).permitAll().anyRequest().authenticated())
   .addFilterBefore(new SessionAuthenticationFilter(sessions),UsernamePasswordAuthenticationFilter.class);
  return http.build();
 }
}
