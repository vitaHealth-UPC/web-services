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
 * When authentication is required, older-adult scoped routes must be accessed by the adult (PIN
 * session) or by a caregiver with an active confirmed Care Link.
 */
@Component
public class OlderAdultOwnershipFilter extends OncePerRequestFilter {
    private static final Pattern OLDER_ADULT_PATH = Pattern.compile("^/api/v1/older-adults/([^/]+)(/.*)?$");

    private final OlderAdultAccessPort access;
    private final boolean requireAuthentication;

    public OlderAdultOwnershipFilter(
            OlderAdultAccessPort access,
            @Value("${tata.security.require-authentication:false}") boolean requireAuthentication
    ) {
        this.access = access;
        this.requireAuthentication = requireAuthentication;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!requireAuthentication) {
            return true;
        }
        return !OLDER_ADULT_PATH.matcher(path(request)).matches();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Matcher matcher = OLDER_ADULT_PATH.matcher(path(request));
        if (!matcher.matches()) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null) {
            // Spring Security answers with 401 when authentication is required.
            filterChain.doFilter(request, response);
            return;
        }

        String olderAdultId = matcher.group(1);
        if (!access.canAccess(authentication.getName(), olderAdultId)) {
            writeForbidden(response, "OLDER_ADULT_ACCESS_DENIED", "subject is not authorized for this older adult");
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

    private static void writeForbidden(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
