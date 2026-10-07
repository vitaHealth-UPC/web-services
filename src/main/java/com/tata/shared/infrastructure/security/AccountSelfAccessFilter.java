package com.tata.shared.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Prevents a signed-in account from reading or changing another account's subscription.
 */
@Component
public class AccountSelfAccessFilter extends OncePerRequestFilter {
    private static final Pattern ACCOUNT_SUBSCRIPTION_PATH =
            Pattern.compile("^/api/v1/accounts/([^/]+)/subscription$");

    private final boolean requireAuthentication;

    public AccountSelfAccessFilter(
            @Value("${tata.security.require-authentication:false}") boolean requireAuthentication
    ) {
        this.requireAuthentication = requireAuthentication;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!requireAuthentication) {
            return true;
        }
        return !ACCOUNT_SUBSCRIPTION_PATH.matcher(path(request)).matches();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Matcher matcher = ACCOUNT_SUBSCRIPTION_PATH.matcher(path(request));
        if (!matcher.matches()) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authentication.getName().equals(matcher.group(1))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"code\":\"ACCOUNT_ACCESS_DENIED\",\"message\":\"session subject does not own this account\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static String path(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }
}
