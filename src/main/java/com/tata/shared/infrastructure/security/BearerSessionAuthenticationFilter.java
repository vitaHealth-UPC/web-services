package com.tata.shared.infrastructure.security;

import com.tata.identitysubscription.application.internal.outboundservices.SessionTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves opaque session tokens issued by Identity & Subscription into the Spring Security context.
 */
@Component
public class BearerSessionAuthenticationFilter extends OncePerRequestFilter {
    private final SessionTokenService sessionTokens;

    public BearerSessionAuthenticationFilter(SessionTokenService sessionTokens) {
        this.sessionTokens = sessionTokens;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
                sessionTokens.authenticate(authorization.substring(7).trim()).ifPresent(subject -> {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            subject.subjectId(),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_SESSION"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }
        }
        filterChain.doFilter(request, response);
    }
}
